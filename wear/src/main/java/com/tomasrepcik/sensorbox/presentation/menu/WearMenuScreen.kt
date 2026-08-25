package com.tomasrepcik.sensorbox.presentation.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.presentation.WearListScreen
import com.tomasrepcik.sensorbox.presentation.WearNavigationRow
import com.tomasrepcik.sensorbox.presentation.WearPageTitle

@Composable
fun WearMenuScreen(state: WearMenuState, onDestinationSelected: (WearMenuDestination) -> Unit) {
    WearListScreen { transformation ->
        item { WearPageTitle(stringResource(R.string.app_name), transformation) }
        state.destinations.forEach { destination ->
            item {
                WearNavigationRow(
                    label = stringResource(destination.labelResource),
                    icon = destination.iconResource,
                    transformation = transformation,
                ) { onDestinationSelected(destination) }
            }
        }
    }
}
