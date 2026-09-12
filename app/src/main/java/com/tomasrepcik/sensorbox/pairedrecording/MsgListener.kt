package com.tomasrepcik.sensorbox.pairedrecording

import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tomasrepcik.sensorbox.measurements.sync.ReceiveWatchFileUseCase
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileTransferClient
import com.tomasrepcik.sensorbox.wearoslib.sync.WearTransferWorkService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MsgListener : WearableListenerService() {
    @Inject
    lateinit var messageDispatcher: PhoneWatchMessageDispatcher

    @Inject
    lateinit var receiveWearFile: ReceiveWatchFileUseCase

    @Inject
    lateinit var transfers: WearFileTransferClient

    override fun onMessageReceived(messageEvent: MessageEvent) {
        runBlocking { messageDispatcher.dispatch(messageEvent.path, messageEvent.data) }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        WearTransferWorkService.enqueue(this) { receiveWearFile(channel) }
            .onFailure { runBlocking { transfers.reject(channel) } }
    }
}
