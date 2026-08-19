package com.motionapps.sensorbox.communication

import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.preferences.AppPreferences
import com.motionapps.sensorbox.domain.measurement.WearRecordingController
import com.motionapps.wearoslib.WearOsConstants.PHONE_APP_CAPABILITY
import com.motionapps.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.motionapps.wearoslib.protocol.SendWearCommandUseCase
import com.motionapps.wearoslib.protocol.WearAcknowledgementOutcome
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearRecordingRequest
import com.motionapps.wearoslib.protocol.WearSessionCommand
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class WearCommandHandler @Inject constructor(
    private val recordingController: WearRecordingController,
    private val environment: WearCommandEnvironment,
    private val sendCommand: SendWearCommandUseCase,
    private val acknowledgementInbox: WearAcknowledgementInbox,
) {
    private val mutex = Mutex()
    private val acknowledgements = mutableMapOf<AcknowledgementKey, WearCommand.Acknowledgement>()
    private var preparedSession: PreparedSession? = null
    private var activeSessionId: String? = null

    suspend fun handle(command: WearCommand): AppResult<Unit> = when (command) {
        is WearCommand.PrepareRecording -> sendAcknowledgement(prepare(command))

        is WearCommand.CommitRecording -> sendAcknowledgement(commit(command))

        is WearCommand.AbortRecording -> sendAcknowledgement(abort(command))

        is WearCommand.StopRecording -> sendAcknowledgement(stop(command))

        WearCommand.RequestSensorList -> sendSensorList()

        is WearCommand.Acknowledgement -> {
            acknowledgementInbox.publish(command)
            AppResult.success(Unit)
        }

        WearCommand.LaunchPhone,
        WearCommand.SyncMeasurements,
        is WearCommand.SensorList,
        -> AppResult.success(Unit)
    }

    suspend fun onAutomaticStop(reason: com.motionapps.wearoslib.protocol.WearStopReason): AppResult<Unit> {
        val sessionId = mutex.withLock {
            activeSessionId?.also(::clearSession) ?: return AppResult.success(Unit)
        }
        val command = WearCommand.StopRecording(sessionId, reason)
        acknowledgementInbox.clear(sessionId, WearSessionCommand.STOP)
        repeat(ATTEMPT_COUNT) {
            if (sendCommand(PHONE_APP_CAPABILITY, PHONE_MESSAGE_PATH, command).isSuccess) {
                val acknowledgement = withTimeoutOrNull(STOP_TIMEOUT_MILLIS / ATTEMPT_COUNT) {
                    acknowledgementInbox.await(sessionId, WearSessionCommand.STOP)
                }
                if (acknowledgement != null) {
                    return if (acknowledgement.outcome == WearAcknowledgementOutcome.SUCCEEDED) {
                        AppResult.success(Unit)
                    } else {
                        AppResult.failure(
                            AppError(
                                acknowledgement.errorCode ?: AppErrorCode.UNKNOWN,
                                "Propagate automatic Wear stop",
                            ),
                        )
                    }
                }
            }
        }
        return AppResult.failure(
            AppError(
                code = AppErrorCode.TIMEOUT,
                operation = "Propagate automatic Wear stop",
                diagnosticMessage = "Phone stop acknowledgement timed out",
                context = mapOf("sessionId" to sessionId, "retryCount" to RETRY_COUNT.toString()),
                isRetryable = true,
            ),
        )
    }

    private suspend fun prepare(command: WearCommand.PrepareRecording): WearCommand.Acknowledgement = mutex.withLock {
        cached(command.sessionId, WearSessionCommand.PREPARE)?.let { return@withLock it }
        val result = validatePrepare(command)
        if (result is AppResult.Success) {
            preparedSession = PreparedSession(command.sessionId, command.request, result.value)
        }
        acknowledgement(
            sessionId = command.sessionId,
            command = WearSessionCommand.PREPARE,
            result = result.map { Unit },
            failureOutcome = WearAcknowledgementOutcome.REJECTED,
        )
    }

    private suspend fun validatePrepare(command: WearCommand.PrepareRecording): AppResult<AppPreferences> {
        val occupiedSession = activeSessionId ?: preparedSession?.sessionId
        if (occupiedSession != null && occupiedSession != command.sessionId) {
            return AppResult.failure(AppError(AppErrorCode.CONFLICT, "Prepare Wear recording"))
        }
        return environment.prepare(command.request)
    }

    private suspend fun commit(command: WearCommand.CommitRecording): WearCommand.Acknowledgement = mutex.withLock {
        cached(command.sessionId, WearSessionCommand.COMMIT)?.let { return@withLock it }
        val prepared = preparedSession
        val result = if (activeSessionId == command.sessionId) {
            AppResult.success(Unit)
        } else if (prepared?.sessionId != command.sessionId) {
            AppResult.failure(AppError(AppErrorCode.CONFLICT, "Commit unprepared Wear recording"))
        } else {
            recordingController.start(
                sessionId = command.sessionId,
                request = prepared.request,
                preferences = prepared.preferences,
                startAtEpochMillis = command.startAtEpochMillis,
            ).onSuccess {
                activeSessionId = command.sessionId
                preparedSession = null
            }
        }
        acknowledgement(command.sessionId, WearSessionCommand.COMMIT, result)
    }

    private suspend fun abort(command: WearCommand.AbortRecording): WearCommand.Acknowledgement = mutex.withLock {
        cached(command.sessionId, WearSessionCommand.ABORT)?.let { return@withLock it }
        val result = when {
            activeSessionId == command.sessionId -> recordingController.stop(
                command.sessionId,
                com.motionapps.wearoslib.protocol.WearStopReason.PAIRED_ABORT,
            )

            preparedSession?.sessionId == command.sessionId -> AppResult.success(Unit)

            else -> AppResult.success(Unit)
        }
        if (result.isSuccess) clearSession(command.sessionId)
        acknowledgement(command.sessionId, WearSessionCommand.ABORT, result)
    }

    private suspend fun stop(command: WearCommand.StopRecording): WearCommand.Acknowledgement = mutex.withLock {
        cached(command.sessionId, WearSessionCommand.STOP)?.let { return@withLock it }
        val result = when {
            activeSessionId == command.sessionId -> recordingController.stop(command.sessionId, command.reason)
            preparedSession?.sessionId == command.sessionId -> AppResult.success(Unit)
            else -> AppResult.success(Unit)
        }
        if (result.isSuccess) clearSession(command.sessionId)
        acknowledgement(command.sessionId, WearSessionCommand.STOP, result)
    }

    private fun acknowledgement(
        sessionId: String,
        command: WearSessionCommand,
        result: AppResult<Unit>,
        failureOutcome: WearAcknowledgementOutcome = WearAcknowledgementOutcome.FAILED,
    ): WearCommand.Acknowledgement {
        val acknowledgement = WearCommand.Acknowledgement(
            sessionId = sessionId,
            command = command,
            outcome = if (result.isSuccess) WearAcknowledgementOutcome.SUCCEEDED else failureOutcome,
            errorCode = result.errorOrNull()?.code,
            failureCount = if (result.isFailure) 1 else 0,
        )
        acknowledgements[AcknowledgementKey(sessionId, command)] = acknowledgement
        return acknowledgement
    }

    private fun cached(sessionId: String, command: WearSessionCommand): WearCommand.Acknowledgement? =
        acknowledgements[AcknowledgementKey(sessionId, command)]

    private fun clearSession(sessionId: String) {
        if (preparedSession?.sessionId == sessionId) preparedSession = null
        if (activeSessionId == sessionId) activeSessionId = null
    }

    private suspend fun sendSensorList(): AppResult<Unit> = sendCommand(
        PHONE_APP_CAPABILITY,
        PHONE_MESSAGE_PATH,
        WearCommand.SensorList(environment.sensors()),
    )

    private suspend fun sendAcknowledgement(acknowledgement: WearCommand.Acknowledgement): AppResult<Unit> =
        sendCommand(PHONE_APP_CAPABILITY, PHONE_MESSAGE_PATH, acknowledgement)

    private data class PreparedSession(
        val sessionId: String,
        val request: WearRecordingRequest,
        val preferences: AppPreferences,
    )

    private data class AcknowledgementKey(val sessionId: String, val command: WearSessionCommand)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val RETRY_COUNT = 2
        const val ATTEMPT_COUNT = RETRY_COUNT + 1
    }
}
