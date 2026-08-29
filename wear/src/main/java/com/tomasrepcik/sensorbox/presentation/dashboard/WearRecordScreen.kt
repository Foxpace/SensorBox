package com.tomasrepcik.sensorbox.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.presentation.WearListScreen
import com.tomasrepcik.sensorbox.presentation.WearPageTitle
import com.tomasrepcik.sensorbox.presentation.WearSectionTitle

@Composable
internal fun WearRecordScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    val selectedCount = state.selectedSensorIds.size + if (state.includesGps) 1 else 0
    WearListScreen(
        edgeButton = { WearRecordingStartButton(selectedCount, accept) },
    ) { transformation ->
        item { WearPageTitle(stringResource(R.string.sources), transformation) }
        item { WearGpsSourceRow(state.includesGps, transformation) { accept(WearDashboardIntent.ToggleGps) } }
        state.sensors.forEach { sensor ->
            item {
                WearSensorSourceRow(
                    sensor = sensor,
                    selected = sensor.type in state.selectedSensorIds,
                    transformation = transformation,
                ) { accept(WearDashboardIntent.ToggleSensor(sensor.type)) }
            }
        }
        state.message?.let { message ->
            item { WearSectionTitle(wearMessageText(message), transformation) }
        }
        item { WearBackButton(transformation, accept) }
    }
}
