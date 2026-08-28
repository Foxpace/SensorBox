package com.tomasrepcik.sensorbox.domain.paired

import android.content.Context
import android.content.Intent
import com.tomasrepcik.sensorbox.activities.MainActivity
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.domain.sensors.WatchSensorCatalogStore
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOperation
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOutcome
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PhoneWatchCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val watchSensorCatalog: WatchSensorCatalogStore,
    private val recordingResults: WatchRecordingResultInbox,
    private val pairedRecordingCoordinator: PairedRecordingCoordinator,
    private val sendCommand: SendWearCommandUseCase,
) {
    suspend fun handle(command: WearCommand): AppResult<Unit> = when (command) {
        WearCommand.LaunchPhone -> launchPhone()

        is WearCommand.AvailableSensors -> {
            watchSensorCatalog.update(command.sensors)
            AppResult.success(Unit)
        }

        is WearCommand.RecordingResult -> {
            recordingResults.publish(command)
            AppResult.success(Unit)
        }

        is WearCommand.StopRecording -> stopFromWatch(command)

        WearCommand.RequestAvailableSensors,
        WearCommand.SyncMeasurements,
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
        val stopResult = pairedRecordingCoordinator.stopFromWatch(command.sessionId, command.reason)
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
