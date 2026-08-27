package com.tomasrepcik.sensorbox.presentation.phonelaunch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.PHONE_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.connectivity.ObserveWearCapabilityUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneLaunchViewModel @Inject constructor(
    private val observeWearCapability: ObserveWearCapabilityUseCase,
    private val sendWearCommand: SendWearCommandUseCase,
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
            val result = sendWearCommand(PHONE_APP_CAPABILITY, PHONE_MESSAGE_PATH, WearCommand.LaunchPhone)
            accept(PhoneLaunchIntent.LaunchCompleted(result.isSuccess))
        }
    }
}
