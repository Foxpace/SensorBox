package com.motionapps.sensorbox.presentation.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.presentation.WearListScreen
import com.motionapps.sensorbox.presentation.WearNavigationRow
import com.motionapps.sensorbox.presentation.WearPageTitle

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
