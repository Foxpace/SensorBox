package com.motionapps.sensorbox.presentation.phonelaunch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionapps.sensorbox.core.error.suspendFlatMap
import com.motionapps.wearoslib.WearOsConstants.PHONE_APP_CAPABILITY
import com.motionapps.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.motionapps.wearoslib.connectivity.ObserveWearCapabilityUseCase
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import com.motionapps.wearoslib.connectivity.WearConnection
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneLaunchViewModel @Inject constructor(
    private val observeWearCapability: ObserveWearCapabilityUseCase,
    private val sendWearMessage: SendWearMessageUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PhoneLaunchState())
    val state: StateFlow<PhoneLaunchState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            observeWearCapability(PHONE_APP_CAPABILITY).collect { connection ->
                accept(PhoneLaunchIntent.ConnectionChanged(connection is WearConnection.Connected))
            }
        }
    }

    fun accept(intent: PhoneLaunchIntent) {
        val next = PhoneLaunchReducer.reduce(mutableState.value, intent)
        mutableState.value = next.state
        if (next.effect == PhoneLaunchEffect.SendLaunchMessage) {
            sendLaunchMessage()
        }
    }

    private fun sendLaunchMessage() {
        viewModelScope.launch {
            val result = WearCommandCodec.encode(WearCommand.LaunchPhone).suspendFlatMap { payload ->
                sendWearMessage(PHONE_APP_CAPABILITY, PHONE_MESSAGE_PATH, payload)
            }
            accept(PhoneLaunchIntent.LaunchCompleted(result.isSuccess))
        }
    }
}
