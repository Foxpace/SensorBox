package com.tomasrepcik.sensorbox.presentation.phonelaunch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppFailureStore
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhoneLaunchViewModel @Inject constructor(
    private val observeWearCapability: ObserveWearCapabilityUseCase,
    private val sendWearCommand: SendWearCommandUseCase,
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PhoneLaunchState())
    val state: StateFlow<PhoneLaunchState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            appFailures.visibleFailure.collect { error ->
                mutableState.value = state.value.copy(visibleFailureCode = error?.code)
            }
        }
        viewModelScope.launch {
            observeWearCapability(PHONE_APP_CAPABILITY).catch { cause ->
                appFailures.show(
                    AppError(
                        AppErrorCode.CONNECTIVITY,
                        "Observe phone connection",
                        "Phone connection observation failed",
                        cause,
                    ),
                )
            }.collect { connection ->
                accept(PhoneLaunchIntent.ConnectionChanged(connection is WearConnection.Connected))
            }
        }
    }

    fun accept(intent: PhoneLaunchIntent) {
        if (intent == PhoneLaunchIntent.DismissFailure) appFailures.dismiss()
        val next = PhoneLaunchReducer.reduce(mutableState.value, intent)
        mutableState.value = next.state
        if (next.effect == PhoneLaunchEffect.SendLaunchMessage) {
            sendLaunchMessage()
        }
    }

    private fun sendLaunchMessage() {
        viewModelScope.launch {
            val result = sendWearCommand(PHONE_APP_CAPABILITY, PHONE_MESSAGE_PATH, WearCommand.LaunchPhone)
            result.errorOrNull()?.let(appFailures::show)
            accept(PhoneLaunchIntent.LaunchCompleted(result.isSuccess))
        }
    }
}
