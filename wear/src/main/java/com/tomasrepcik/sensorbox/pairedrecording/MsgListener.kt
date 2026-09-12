package com.tomasrepcik.sensorbox.pairedrecording

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.sync.WearTransferWorkService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MsgListener : WearableListenerService() {
    @Inject
    lateinit var messageDispatcher: WearMessageDispatcher

    @Inject
    lateinit var syncLock: MeasurementSyncLock

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val payload = messageEvent.data.copyOf()
        val command = WearCommandCodec.decode(payload).getOrNull()
        if (path == WEAR_MESSAGE_PATH && command is WearCommand.CopyWatchMeasurements) {
            syncLock.begin(command.requestId)
            WearTransferWorkService.enqueue(this) {
                try {
                    messageDispatcher.dispatch(path, payload)
                } finally {
                    syncLock.finish(command.requestId)
                }
            }.onFailure { syncLock.finish(command.requestId) }
        } else {
            runBlocking { messageDispatcher.dispatch(path, payload) }
        }
    }
}
