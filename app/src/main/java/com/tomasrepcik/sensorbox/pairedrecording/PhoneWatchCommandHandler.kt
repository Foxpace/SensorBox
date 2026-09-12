package com.tomasrepcik.sensorbox.pairedrecording

import android.content.Context
import android.content.Intent
import com.tomasrepcik.sensorbox.bootstrap.MainActivity
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.measurements.sync.WatchSyncRepo
import com.tomasrepcik.sensorbox.recording.PeerRecordingControl
import com.tomasrepcik.sensorbox.recording.sources.ReceiveWatchSensorsUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.RecordingResultReceiver
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingOperation
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingOutcome
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PhoneWatchCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val receiveWatchSensors: ReceiveWatchSensorsUseCase,
    private val recordingResults: RecordingResultReceiver,
    private val recording: PeerRecordingControl,
    private val sendCommand: SendWearCommandUseCase,
    private val watchSync: WatchSyncRepo,
) {
    suspend fun handle(command: WearCommand): AppResult<Unit> = when (command) {
        WearCommand.LaunchPhone -> launchPhone()

        is WearCommand.AvailableSensors -> {
            receiveWatchSensors.receive(command.sensors)
            AppResult.success(Unit)
        }

        is WearCommand.RecordingResult -> {
            recordingResults.receive(command)
            AppResult.success(Unit)
        }

        is WearCommand.StopRecording -> stopFromWatch(command)

        is WearCommand.WatchMeasurementsStatus -> {
            watchSync.receive(command)
            AppResult.success(Unit)
        }

        is WearCommand.CheckWatchMeasurements,
        is WearCommand.CopyWatchMeasurements,
        is WearCommand.CancelWatchSync,
        WearCommand.RequestAvailableSensors,
        is WearCommand.StartRecording,
        -> AppResult.failure(AppError(AppErrorCode.VALIDATION, "Handle unsupported phone watch command"))
    }

    private fun launchPhone(): AppResult<Unit> =
        appResult(AppErrorCode.EXTERNAL_ACTION, "Launch phone app from watch") {
            context.startActivity(
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
            )
        }

    private suspend fun stopFromWatch(command: WearCommand.StopRecording): AppResult<Unit> {
        val stopResult = recording.stopFromWatch(command.sessionId, command.reason)
        val error = stopResult.errorOrNull()
        val result = WearCommand.RecordingResult(
            sessionId = command.sessionId,
            operation = WearRecordingOperation.STOP,
            outcome = if (stopResult.isSuccess) {
                WearRecordingOutcome.SUCCEEDED
            } else {
                WearRecordingOutcome.FAILED
            },
            errorCode = error?.code,
            errorOperation = error?.operation,
            errorMessage = error?.diagnosticMessage,
            errorContext = error?.context.orEmpty(),
            failureCount = if (stopResult.isFailure) 1 else 0,
        )
        return sendCommand(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, result)
    }
}
