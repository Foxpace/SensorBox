package com.tomasrepcik.sensorbox.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.WearListScreen
import com.tomasrepcik.sensorbox.design.WearNavigationRow
import com.tomasrepcik.sensorbox.design.WearPageTitle

@Composable
fun WearMenuScreen(state: WearMenuState, onDestinationSelected: (WearMenuDestination) -> Unit) {
    WearListScreen { transformation ->
        item { WearPageTitle(stringResource(R.string.app_name), transformation) }
        state.destinations.forEach { destination ->
            item {
                WearNavigationRow(
                    label = stringResource(destination.labelResource()),
                    icon = destination.iconResource(),
                    transformation = transformation,
                ) { onDestinationSelected(destination) }
            }
        }
    }
}

@StringRes
private fun WearMenuDestination.labelResource(): Int = when (this) {
    WearMenuDestination.RECORD -> R.string.activity_record
    WearMenuDestination.LIVE_SENSOR -> R.string.activity_view_sensor
    WearMenuDestination.PHONE_INFO -> R.string.activity_info_phone
    WearMenuDestination.SETTINGS -> R.string.activity_settings
    WearMenuDestination.PRIVACY -> R.string.activity_privacy_policy
    WearMenuDestination.TERMS -> R.string.activity_terms_of_use
}

@DrawableRes
private fun WearMenuDestination.iconResource(): Int = when (this) {
    WearMenuDestination.RECORD -> R.drawable.ic_record
    WearMenuDestination.LIVE_SENSOR -> R.drawable.ic_live
    WearMenuDestination.PHONE_INFO -> R.drawable.ic_phone
    WearMenuDestination.SETTINGS -> R.drawable.ic_settings
    WearMenuDestination.PRIVACY -> R.drawable.ic_privacy
    WearMenuDestination.TERMS -> R.drawable.ic_terms
}
