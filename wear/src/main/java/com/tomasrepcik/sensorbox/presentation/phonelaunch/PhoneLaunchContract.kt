package com.tomasrepcik.sensorbox.presentation.phonelaunch

enum class PhoneLaunchStatus {
    IDLE,
    PHONE_UNAVAILABLE,
    LAUNCHING,
    SENT,
    FAILED,
}

data class PhoneLaunchState(
    val isPhoneConnected: Boolean = false,
    val status: PhoneLaunchStatus = PhoneLaunchStatus.IDLE,
)

sealed interface PhoneLaunchIntent {
    data object LaunchRequested : PhoneLaunchIntent

    data class ConnectionChanged(val isConnected: Boolean) : PhoneLaunchIntent

    data class LaunchCompleted(val succeeded: Boolean) : PhoneLaunchIntent
}

sealed interface PhoneLaunchEffect {
    data object SendLaunchMessage : PhoneLaunchEffect
}

data class PhoneLaunchNext(val state: PhoneLaunchState, val effect: PhoneLaunchEffect? = null)

object PhoneLaunchReducer {
    fun reduce(state: PhoneLaunchState, intent: PhoneLaunchIntent): PhoneLaunchNext = when (intent) {
        is PhoneLaunchIntent.ConnectionChanged -> connectionChanged(state, intent.isConnected)
        is PhoneLaunchIntent.LaunchCompleted -> launchCompleted(state, intent.succeeded)
        PhoneLaunchIntent.LaunchRequested -> launchRequested(state)
    }

    private fun connectionChanged(state: PhoneLaunchState, isConnected: Boolean) = PhoneLaunchNext(
        state.copy(
            isPhoneConnected = isConnected,
            status = if (isConnected) PhoneLaunchStatus.IDLE else PhoneLaunchStatus.PHONE_UNAVAILABLE,
        ),
    )

    private fun launchCompleted(state: PhoneLaunchState, succeeded: Boolean) = PhoneLaunchNext(
        state.copy(status = if (succeeded) PhoneLaunchStatus.SENT else PhoneLaunchStatus.FAILED),
    )

    private fun launchRequested(state: PhoneLaunchState): PhoneLaunchNext = if (state.isPhoneConnected) {
        PhoneLaunchNext(
            state = state.copy(status = PhoneLaunchStatus.LAUNCHING),
            effect = PhoneLaunchEffect.SendLaunchMessage,
        )
    } else {
        PhoneLaunchNext(state.copy(status = PhoneLaunchStatus.PHONE_UNAVAILABLE))
    }
}
