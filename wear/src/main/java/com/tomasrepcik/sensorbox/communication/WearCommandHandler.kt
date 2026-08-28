package com.tomasrepcik.sensorbox.communication

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.domain.recording.WatchRecordingControlUseCase
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.PHONE_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOperation
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOutcome
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearStopReason
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class WearCommandHandler @Inject constructor(
    private val recording: WatchRecordingControlUseCase,
    private val environment: WearRecordingRequirementsUseCase,
    private val sendCommand: SendWearCommandUseCase,
    private val phoneResults: PhoneRecordingResultInbox,
) {
    private val completedResults = mutableMapOf<ResultKey, WearCommand.RecordingResult>()
    private var activeSessionId: String? = null

    suspend fun handle(command: WearCommand): AppResult<Unit> = when (command) {
        is WearCommand.StartRecording -> sendResult(start(command))

        is WearCommand.StopRecording -> sendResult(stop(command))

        WearCommand.RequestAvailableSensors -> sendAvailableSensors()

        is WearCommand.RecordingResult -> {
            phoneResults.publish(command)
            AppResult.success(Unit)
        }

        WearCommand.LaunchPhone,
        WearCommand.SyncMeasurements,
        is WearCommand.AvailableSensors,
        -> AppResult.success(Unit)
    }

    suspend fun onAutomaticStop(reason: WearStopReason): AppResult<Unit> {
        val sessionId = activeSessionId ?: return AppResult.success(Unit)
        activeSessionId = null
        val command = WearCommand.StopRecording(sessionId, reason)
        phoneResults.clear(sessionId, WearRecordingOperation.STOP)
        var lastSendError: AppError? = null

        repeat(ATTEMPT_COUNT) { retryCount ->
            when (val sent = sendCommand(PHONE_APP_CAPABILITY, PHONE_MESSAGE_PATH, command)) {
                is AppResult.Failure -> lastSendError = sent.error

                is AppResult.Success -> {
                    lastSendError = null
                    val result = withTimeoutOrNull(RESULT_TIMEOUT_MILLIS / ATTEMPT_COUNT) {
                        phoneResults.await(sessionId, WearRecordingOperation.STOP)
                    }
                    if (result != null) return result.toAppResult(retryCount)
                }
            }
        }

        return lastSendError?.let { error -> AppResult.failure(error) } ?: AppResult.failure(
            AppError(
                code = AppErrorCode.TIMEOUT,
                operation = "Propagate automatic Wear stop",
                diagnosticMessage = "Phone stop result timed out",
                context = mapOf(
                    "sessionId" to sessionId,
                    "retryCount" to RETRY_COUNT.toString(),
                ),
                isRetryable = true,
            ),
        )
    }

    private suspend fun start(command: WearCommand.StartRecording): WearCommand.RecordingResult {
        cached(command.sessionId, WearRecordingOperation.START)?.let { return it }

        val result = when (val active = activeSessionId) {
            command.sessionId -> AppResult.success(Unit)

            null -> when (val validation = environment.validate(command.request)) {
                is AppResult.Failure -> validation

                is AppResult.Success -> recording.start(
                    sessionId = command.sessionId,
                    request = command.request,
                    preferences = validation.value,
                ).onSuccess {
                    activeSessionId = command.sessionId
                }
            }

            else -> AppResult.failure(
                AppError(
                    code = AppErrorCode.CONFLICT,
                    operation = "Start watch recording",
                    diagnosticMessage = "Another watch recording is active",
                    context = mapOf("activeSessionId" to active),
                ),
            )
        }

        return remember(command.sessionId, WearRecordingOperation.START, result)
    }

    private fun stop(command: WearCommand.StopRecording): WearCommand.RecordingResult {
        cached(command.sessionId, WearRecordingOperation.STOP)?.let { return it }

        val result = if (activeSessionId == command.sessionId) {
            recording.stop(command.sessionId, command.reason).onSuccess {
                activeSessionId = null
            }
        } else {
            AppResult.success(Unit)
        }

        return remember(command.sessionId, WearRecordingOperation.STOP, result)
    }

    private suspend fun sendAvailableSensors(): AppResult<Unit> = sendCommand(
        PHONE_APP_CAPABILITY,
        PHONE_MESSAGE_PATH,
        WearCommand.AvailableSensors(environment.availableSensors()),
    )

    private suspend fun sendResult(result: WearCommand.RecordingResult): AppResult<Unit> = sendCommand(
        PHONE_APP_CAPABILITY,
        PHONE_MESSAGE_PATH,
        result,
    )

    private fun remember(
        sessionId: String,
        operation: WearRecordingOperation,
        result: AppResult<Unit>,
    ): WearCommand.RecordingResult {
        val error = result.errorOrNull()
        return WearCommand.RecordingResult(
            sessionId = sessionId,
            operation = operation,
            outcome = if (result.isSuccess) WearRecordingOutcome.SUCCEEDED else WearRecordingOutcome.FAILED,
            errorCode = error?.code,
            errorOperation = error?.operation,
            errorMessage = error?.diagnosticMessage,
            errorContext = error?.context.orEmpty(),
            failureCount = if (result.isFailure) 1 else 0,
        ).also { completedResults[ResultKey(sessionId, operation)] = it }
    }

    private fun cached(sessionId: String, operation: WearRecordingOperation): WearCommand.RecordingResult? =
        completedResults[ResultKey(sessionId, operation)]

    private fun WearCommand.RecordingResult.toAppResult(retryCount: Int): AppResult<Unit> =
        if (outcome == WearRecordingOutcome.SUCCEEDED) {
            AppResult.success(Unit)
        } else {
            AppResult.failure(
                AppError(
                    code = errorCode ?: AppErrorCode.UNKNOWN,
                    operation = errorOperation ?: "Handle phone $operation result",
                    diagnosticMessage = errorMessage ?: "Phone $operation failed",
                    context = errorContext + mapOf(
                        "source" to "phone",
                        "sessionId" to sessionId,
                        "retryCount" to retryCount.toString(),
                        "failureCount" to failureCount.toString(),
                    ),
                ),
            )
        }

    private data class ResultKey(val sessionId: String, val operation: WearRecordingOperation)

    private companion object {
        const val RESULT_TIMEOUT_MILLIS = 5_000L
        const val RETRY_COUNT = 2
        const val ATTEMPT_COUNT = RETRY_COUNT + 1
    }
}
