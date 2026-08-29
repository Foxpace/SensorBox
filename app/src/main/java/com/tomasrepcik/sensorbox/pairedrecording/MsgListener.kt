package com.tomasrepcik.sensorbox.pairedrecording

import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tomasrepcik.sensorbox.measurements.sync.ReceiveWatchFileUseCase
import com.tomasrepcik.sensorbox.pairedrecording.PhoneWatchMessageDispatcher
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
    lateinit var messageDispatcher: PhoneWatchMessageDispatcher

    @Inject
    lateinit var receiveWearFile: ReceiveWatchFileUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        serviceScope.launch { messageDispatcher.dispatch(messageEvent.path, messageEvent.data) }
    }

    override fun onChannelOpened(channel: ChannelClient.Channel) {
        serviceScope.launch { receiveWearFile(channel) }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
