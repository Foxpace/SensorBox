package com.tomasrepcik.sensorbox.domain.paired

import android.content.Context
import android.content.Intent
import com.tomasrepcik.sensorbox.activities.MainActivity
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.domain.sensors.WearSensorCatalogStore
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearAcknowledgementOutcome
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearSessionCommand
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

fun interface PhoneWearCommandPolicy {
    suspend fun handle(command: WearCommand): AppResult<Unit>
}

class PhoneWearCommandHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wearSensorCatalog: WearSensorCatalogStore,
    private val acknowledgementInbox: WearAcknowledgementInbox,
    private val pairedRecordingCoordinator: PairedRecordingCoordinator,
    private val sendCommand: SendWearCommandUseCase,
) : PhoneWearCommandPolicy {
    override suspend fun handle(command: WearCommand): AppResult<Unit> = when (command) {
        WearCommand.LaunchPhone -> launchPhone()

        is WearCommand.SensorList -> {
            wearSensorCatalog.update(command.sensors)
            AppResult.success(Unit)
        }

        is WearCommand.Acknowledgement -> {
            acknowledgementInbox.publish(command)
            AppResult.success(Unit)
        }

        is WearCommand.StopRecording -> stopFromWear(command)

        WearCommand.RequestSensorList,
        WearCommand.SyncMeasurements,
        is WearCommand.PrepareRecording,
        is WearCommand.CommitRecording,
        is WearCommand.AbortRecording,
        -> AppResult.failure(AppError(AppErrorCode.VALIDATION, "Handle unsupported phone Wear command"))
    }

    private fun launchPhone(): AppResult<Unit> = appResult(AppErrorCode.EXTERNAL_ACTION, "Launch phone app from Wear") {
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
        )
    }

    private suspend fun stopFromWear(command: WearCommand.StopRecording): AppResult<Unit> {
        val stopResult = pairedRecordingCoordinator.stopFromPeer(command.sessionId, command.reason)
        val acknowledgement = WearCommand.Acknowledgement(
            sessionId = command.sessionId,
            command = WearSessionCommand.STOP,
            outcome = if (stopResult.isSuccess) {
                WearAcknowledgementOutcome.SUCCEEDED
            } else {
                WearAcknowledgementOutcome.FAILED
            },
            errorCode = stopResult.errorOrNull()?.code,
            failureCount = if (stopResult.isFailure) 1 else 0,
        )
        return sendCommand(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, acknowledgement)
    }
}
