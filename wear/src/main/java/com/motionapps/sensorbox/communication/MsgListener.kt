package com.motionapps.sensorbox.communication

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
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
    lateinit var messageDispatcher: WearMessageDispatcher

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        serviceScope.launch { messageDispatcher.dispatch(messageEvent.path, messageEvent.data) }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
