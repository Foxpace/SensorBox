package com.tomasrepcik.sensorbox.presentation.menu

enum class WearMenuDestination {
    RECORD,
    SYNC,
    LIVE_SENSOR,
    PHONE_INFO,
    SETTINGS,
    PRIVACY,
    TERMS,
}

data class WearMenuState(val destinations: List<WearMenuDestination> = WearMenuDestination.entries)

sealed interface WearMenuIntent {
    data class SelectDestination(val destination: WearMenuDestination) : WearMenuIntent
}

sealed interface WearMenuEffect {
    data class OpenDestination(val destination: WearMenuDestination) : WearMenuEffect
}

data class WearMenuNext(val state: WearMenuState, val effect: WearMenuEffect? = null)

object WearMenuReducer {
    fun reduce(state: WearMenuState, intent: WearMenuIntent): WearMenuNext = when (intent) {
        is WearMenuIntent.SelectDestination -> WearMenuNext(
            state = state,
            effect = WearMenuEffect.OpenDestination(intent.destination),
        )
    }
}
