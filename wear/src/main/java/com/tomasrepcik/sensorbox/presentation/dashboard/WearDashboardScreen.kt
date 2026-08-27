package com.tomasrepcik.sensorbox.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import com.tomasrepcik.sensorbox.domain.sensors.WearSensorDescriptor
import com.tomasrepcik.sensorbox.presentation.WearCheckboxRow
import com.tomasrepcik.sensorbox.presentation.WearListScreen
import com.tomasrepcik.sensorbox.presentation.WearNavigationRow
import com.tomasrepcik.sensorbox.presentation.WearPageTitle
import com.tomasrepcik.sensorbox.presentation.WearPrimaryAction
import com.tomasrepcik.sensorbox.presentation.WearPrimaryEdgeAction
import com.tomasrepcik.sensorbox.presentation.WearRadioRow
import com.tomasrepcik.sensorbox.presentation.WearSecondaryAction
import com.tomasrepcik.sensorbox.presentation.WearSectionTitle
import com.tomasrepcik.sensorbox.presentation.WearSwitchRow
import com.tomasrepcik.sensorbox.presentation.menu.WearMenuScreen
import com.tomasrepcik.sensorbox.presentation.menu.WearMenuState

@Composable
fun WearDashboardScreen(
    state: WearDashboardState,
    chartModelProducer: CartesianChartModelProducer,
    accept: (WearDashboardIntent) -> Unit,
) {
    when (state.route) {
        WearRoute.MENU -> WearMenuScreen(WearMenuState()) { accept(WearDashboardIntent.Open(it)) }
        WearRoute.RECORD -> WearRecordScreen(state, accept)
        WearRoute.LIVE -> WearLiveScreen(state, chartModelProducer, accept)
        WearRoute.SETTINGS -> WearSettingsScreen(state, accept)
        WearRoute.ACTIVE -> WearActiveScreen(state, accept)
    }
}

@Composable
private fun WearRecordScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    val selectedCount = state.selectedSensorIds.size + if (state.includesGps) 1 else 0
    WearListScreen(
        edgeButton = {
            WearPrimaryEdgeAction(
                label = pluralStringResource(R.plurals.start_source_count, selectedCount, selectedCount),
                enabled = selectedCount > 0,
                onClick = { accept(WearDashboardIntent.StartMeasurement) },
            )
        },
    ) { transformation ->
        item { WearPageTitle(stringResource(R.string.sources), transformation) }
        item {
            WearCheckboxRow(
                label = stringResource(R.string.gps),
                checked = state.includesGps,
                icon = R.drawable.ic_location,
                transformation = transformation,
                onCheckedChange = { accept(WearDashboardIntent.ToggleGps) },
            )
        }
        state.sensors.forEach { sensor ->
            item {
                WearCheckboxRow(
                    label = sensor.name,
                    detail = sensor.vendor,
                    checked = sensor.type in state.selectedSensorIds,
                    icon = R.drawable.ic_sensor,
                    transformation = transformation,
                    onCheckedChange = { accept(WearDashboardIntent.ToggleSensor(sensor.type)) },
                )
            }
        }
        state.message?.let { message ->
            item { WearSectionTitle(wearMessageText(message), transformation) }
        }
        item { WearBackAction(transformation, accept) }
    }
}

@Composable
private fun WearLiveScreen(
    state: WearDashboardState,
    chartModelProducer: CartesianChartModelProducer,
    accept: (WearDashboardIntent) -> Unit,
) {
    val selected = state.sensors.firstOrNull { it.type == state.liveSensorType }
    if (selected == null) {
        WearSensorPicker(state.sensors, accept)
        return
    }
    AppScaffold {
        ScreenScaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = selected.name,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.latestValue?.let(ValueFormats::decimal) ?: stringResource(R.string.waiting),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                CartesianChartHost(
                    chart = rememberCartesianChart(rememberLineCartesianLayer()),
                    modelProducer = chartModelProducer,
                    modifier = Modifier.fillMaxWidth().height(82.dp),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { accept(WearDashboardIntent.Back) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.back))
                }
            }
        }
    }
}

@Composable
private fun WearSensorPicker(sensors: List<WearSensorDescriptor>, accept: (WearDashboardIntent) -> Unit) {
    WearListScreen { transformation ->
        item { WearPageTitle(stringResource(R.string.live_values), transformation) }
        sensors.forEach { sensor ->
            item {
                WearNavigationRow(
                    label = sensor.name,
                    detail = sensor.vendor,
                    icon = R.drawable.ic_sensor,
                    transformation = transformation,
                ) { accept(WearDashboardIntent.ObserveSensor(sensor.type)) }
            }
        }
        item { WearBackAction(transformation, accept) }
    }
}

@Composable
private fun WearSettingsScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    WearListScreen { transformation ->
        item { WearPageTitle(stringResource(R.string.activity_settings), transformation) }
        preferenceItems(state, transformation, accept)
        item { WearSectionTitle(stringResource(R.string.transfer), transformation) }
        item {
            WearPrimaryAction(
                label = stringResource(if (state.isSyncing) R.string.syncing else R.string.sync_recordings),
                transformation = transformation,
                enabled = !state.isSyncing,
            ) { accept(WearDashboardIntent.SyncMeasurements) }
        }
        if (state.isSyncing) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }
        state.message?.let { message ->
            item { WearSectionTitle(wearMessageText(message), transformation) }
        }
        item { WearBackAction(transformation, accept) }
    }
}

private fun TransformingLazyColumnScope.preferenceItems(
    state: WearDashboardState,
    transformation: TransformationSpec,
    accept: (WearDashboardIntent) -> Unit,
) {
    item { WearSectionTitle(stringResource(R.string.sensor_sampling), transformation) }
    listOf(
        R.string.sampling_fastest,
        R.string.sampling_game,
        R.string.sampling_ui,
        R.string.sampling_normal,
    ).forEachIndexed { index, label ->
        item {
            WearRadioRow(
                label = stringResource(label),
                selected = state.preferences.recording.sensorSamplingPeriod == index,
                transformation = transformation,
            ) { accept(WearDashboardIntent.SetSamplingPeriod(index)) }
        }
    }
    item { WearSectionTitle(stringResource(R.string.recording_options), transformation) }
    item {
        WearSwitchRow(
            label = stringResource(R.string.stop_on_low_battery),
            checked = state.preferences.recording.restrictMeasurementOnLowBattery,
            transformation = transformation,
            onCheckedChange = { accept(WearDashboardIntent.ToggleBatteryRestriction) },
        )
    }
    item {
        WearSwitchRow(
            label = stringResource(R.string.wake_lock),
            checked = state.preferences.recording.useWakeLock,
            transformation = transformation,
            onCheckedChange = { accept(WearDashboardIntent.ToggleWakeLock) },
        )
    }
    item {
        WearSwitchRow(
            label = stringResource(R.string.keep_display_on),
            checked = state.preferences.display.keepWearDisplayOn,
            transformation = transformation,
            onCheckedChange = { accept(WearDashboardIntent.ToggleDisplay) },
        )
    }
}

@Composable
private fun WearActiveScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    val selectedCount = state.selectedSensorIds.size + if (state.includesGps) 1 else 0
    AppScaffold {
        ScreenScaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.recording),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = pluralStringResource(R.plurals.source_count, selectedCount, selectedCount),
                    style = MaterialTheme.typography.displaySmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (state.preferences.display.keepWearDisplayOn) {
                            R.string.display_stays_on
                        } else {
                            R.string.display_may_sleep
                        },
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { accept(WearDashboardIntent.StopMeasurement) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.stop_and_save))
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.WearBackAction(
    transformation: TransformationSpec,
    accept: (WearDashboardIntent) -> Unit,
) {
    WearSecondaryAction(stringResource(R.string.back), transformation) {
        accept(WearDashboardIntent.Back)
    }
}

@Composable
private fun wearMessageText(message: WearDashboardMessage): String = when (message) {
    WearDashboardMessage.PickSource -> stringResource(R.string.message_pick_source)

    WearDashboardMessage.PermissionRequired -> stringResource(R.string.message_permission_required)

    WearDashboardMessage.SensorUnavailable -> stringResource(R.string.message_sensor_unavailable)

    WearDashboardMessage.Syncing -> stringResource(R.string.syncing)

    WearDashboardMessage.SyncFailed -> stringResource(R.string.message_sync_failed)

    is WearDashboardMessage.FilesSent ->
        pluralStringResource(R.plurals.message_files_sent, message.count, message.count)
}
