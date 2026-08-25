package com.tomasrepcik.sensorbox

import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.tomasrepcik.sensorbox.domain.paired.PhoneWearMessageDispatcher
import com.tomasrepcik.sensorbox.domain.sync.ReceiveWearFileUseCase
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
    lateinit var messageDispatcher: PhoneWearMessageDispatcher

    @Inject
    lateinit var receiveWearFile: ReceiveWearFileUseCase

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
