package com.motionapps.sensorbox.presentation.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.motionapps.sensorbox.R

enum class WearMenuDestination(@StringRes val labelResource: Int, @DrawableRes val iconResource: Int) {
    RECORD(R.string.activity_record, R.drawable.ic_archive),
    LIVE_SENSOR(R.string.activity_view_sensor, R.drawable.ic_poll),
    PHONE_INFO(R.string.activity_info_phone, R.drawable.ic_info),
    SETTINGS(R.string.activity_settings, R.drawable.ic_more_horiz_24dp_wht),
    PRIVACY(R.string.activity_privacy_policy, R.drawable.ic_incognito),
    TERMS(R.string.activity_terms_of_use, R.drawable.ic_policy),
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
