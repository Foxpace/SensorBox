package com.tomasrepcik.sensorbox.pairedrecording

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.recording.WatchRecordingControlUseCase
import com.tomasrepcik.sensorbox.sync.SyncWatchMeasurementsUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.PHONE_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.PHONE_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.RecordingCommandSender
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.RecordingResultReceiver
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingOperation
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingOutcome
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearStopReason
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearCommandHandler @Inject constructor(
    private val recording: WatchRecordingControlUseCase,
    private val environment: WearRecordingRequirementsUseCase,
    private val sendCommand: SendWearCommandUseCase,
    private val recordingCommands: RecordingCommandSender,
    private val recordingResults: RecordingResultReceiver,
    private val syncMeasurements: SyncWatchMeasurementsUseCase,
) {
    private val completedResults = mutableMapOf<ResultKey, WearCommand.RecordingResult>()
    private var activeSessionId: String? = null

    suspend fun handle(command: WearCommand): AppResult<Unit> = when (command) {
        is WearCommand.StartRecording -> sendResult(start(command))

        is WearCommand.StopRecording -> sendResult(stop(command))

        WearCommand.RequestAvailableSensors -> sendAvailableSensors()

        WearCommand.SyncMeasurements -> syncMeasurements().map { }

        is WearCommand.RecordingResult -> {
            recordingResults.receive(command)
            AppResult.success(Unit)
        }

        WearCommand.LaunchPhone,
        is WearCommand.AvailableSensors,
        -> AppResult.success(Unit)
    }

    suspend fun onAutomaticStop(reason: WearStopReason): AppResult<Unit> {
        val sessionId = activeSessionId ?: return AppResult.success(Unit)
        activeSessionId = null
        return recordingCommands.send(WearCommand.StopRecording(sessionId, reason))
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

    private data class ResultKey(val sessionId: String, val operation: WearRecordingOperation)
}
