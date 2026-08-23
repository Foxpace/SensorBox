package com.motionapps.sensorbox

import android.content.Intent
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.motionapps.sensorbox.activities.MainActivity
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.domain.sync.ReceiveWearFileUseCase
import com.motionapps.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
import com.motionapps.wearoslib.protocol.WearSensorCatalogStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MsgListener : WearableListenerService() {
    @Inject
    lateinit var receiveWearFile: ReceiveWearFileUseCase

    @Inject
    lateinit var wearSensorCatalog: WearSensorCatalogStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != PHONE_MESSAGE_PATH) return
        when (val command = WearCommandCodec.decode(messageEvent.data).getOrNull()) {
            WearCommand.LaunchPhone -> {
                val launchIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                appResult(AppError.Kind.EXTERNAL_ACTION, "Launch phone app from Wear") {
                    startActivity(launchIntent)
                }
            }

            is WearCommand.SensorList -> wearSensorCatalog.update(command.sensors)

            else -> Unit
        }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        serviceScope.launch { receiveWearFile(channel) }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
