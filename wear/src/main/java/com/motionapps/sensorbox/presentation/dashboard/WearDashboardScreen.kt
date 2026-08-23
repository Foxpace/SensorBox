package com.motionapps.sensorbox.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.domain.sensors.WearSensorDescriptor
import com.motionapps.sensorbox.presentation.menu.WearMenuScreen
import com.motionapps.sensorbox.presentation.menu.WearMenuState
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart

@Composable
fun WearDashboardScreen(
    state: WearDashboardState,
    chartModelProducer: CartesianChartModelProducer,
    accept: (WearDashboardIntent) -> Unit,
) {
    when (state.route) {
        WearRoute.MENU -> WearMenuRoute(accept)
        WearRoute.RECORD -> WearRecordScreen(state, accept)
        WearRoute.LIVE -> WearLiveScreen(state, chartModelProducer, accept)
        WearRoute.SETTINGS -> WearSettingsScreen(state, accept)
        WearRoute.ACTIVE -> WearActiveScreen(state.preferences.display.keepWearDisplayOn, accept)
    }
}

@Composable
private fun WearMenuRoute(accept: (WearDashboardIntent) -> Unit) {
    WearMenuScreen(WearMenuState()) { accept(WearDashboardIntent.Open(it)) }
}

@Composable
private fun WearRecordScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformation = rememberTransformationSpec()
    AppScaffold {
        ScreenScaffold(scrollState = listState) { padding ->
            TransformingLazyColumn(state = listState, contentPadding = padding) {
                item { WearHeader(stringResource(R.string.activity_record), transformation) }
                state.sensors.forEach { sensor ->
                    item {
                        WearSensorButton(sensor, sensor.type in state.selectedSensorIds, transformation) {
                            accept(WearDashboardIntent.ToggleSensor(sensor.type))
                        }
                    }
                }
                item {
                    WearChoiceButton(stringResource(R.string.gps), state.includesGps, transformation) {
                        accept(WearDashboardIntent.ToggleGps)
                    }
                }
                state.message?.let { message -> item { WearHeader(wearMessageText(message), transformation) } }
                item {
                    WearActionButton(stringResource(R.string.start_recording), transformation) {
                        accept(WearDashboardIntent.StartMeasurement)
                    }
                }
                item { WearBackButton(transformation, accept) }
            }
        }
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
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(selected.name)
            Text(state.latestValue?.let { "%.2f".format(it) } ?: stringResource(R.string.waiting))
            CartesianChartHost(
                chart = rememberCartesianChart(rememberLineCartesianLayer()),
                modelProducer = chartModelProducer,
                modifier = Modifier.fillMaxWidth().height(92.dp),
            )
            Button(
                label = { Text(stringResource(R.string.back)) },
                onClick = { accept(WearDashboardIntent.Back) },
            )
        }
    }
}

@Composable
private fun WearSensorPicker(sensors: List<WearSensorDescriptor>, accept: (WearDashboardIntent) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformation = rememberTransformationSpec()
    AppScaffold {
        ScreenScaffold(scrollState = listState) { padding ->
            TransformingLazyColumn(state = listState, contentPadding = padding) {
                item { WearHeader(stringResource(R.string.activity_view_sensor), transformation) }
                sensors.forEach { sensor ->
                    item {
                        WearActionButton(sensor.name, transformation) {
                            accept(WearDashboardIntent.ObserveSensor(sensor.type))
                        }
                    }
                }
                item { WearBackButton(transformation, accept) }
            }
        }
    }
}

@Composable
private fun WearSettingsScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val transformation = rememberTransformationSpec()
    AppScaffold {
        ScreenScaffold(scrollState = listState) { padding ->
            TransformingLazyColumn(state = listState, contentPadding = padding) {
                item { WearHeader(stringResource(R.string.activity_settings), transformation) }
                preferenceItems(state, transformation, accept)
                item {
                    WearActionButton(
                        stringResource(if (state.isSyncing) R.string.syncing else R.string.sync_recordings),
                        transformation,
                    ) {
                        accept(WearDashboardIntent.SyncMeasurements)
                    }
                }
                state.message?.let { message -> item { WearHeader(wearMessageText(message), transformation) } }
                item { WearBackButton(transformation, accept) }
            }
        }
    }
}

private fun TransformingLazyColumnScope.preferenceItems(
    state: WearDashboardState,
    transformation: TransformationSpec,
    accept: (WearDashboardIntent) -> Unit,
) {
    listOf(
        R.string.sampling_fastest,
        R.string.sampling_game,
        R.string.sampling_ui,
        R.string.sampling_normal,
    ).forEachIndexed { index, label ->
        item {
            WearChoiceButton(
                stringResource(label),
                state.preferences.recording.sensorSamplingPeriod == index,
                transformation,
            ) {
                accept(WearDashboardIntent.SetSamplingPeriod(index))
            }
        }
    }
    item {
        WearChoiceButton(
            stringResource(R.string.stop_on_low_battery),
            state.preferences.recording.restrictMeasurementOnLowBattery,
            transformation,
        ) {
            accept(WearDashboardIntent.ToggleBatteryRestriction)
        }
    }
    item {
        WearChoiceButton(stringResource(R.string.wake_lock), state.preferences.recording.useWakeLock, transformation) {
            accept(WearDashboardIntent.ToggleWakeLock)
        }
    }
    item {
        WearChoiceButton(
            stringResource(R.string.keep_display_on),
            state.preferences.display.keepWearDisplayOn,
            transformation,
        ) {
            accept(WearDashboardIntent.ToggleDisplay)
        }
    }
}

@Composable
private fun WearActiveScreen(keepDisplayOn: Boolean, accept: (WearDashboardIntent) -> Unit) {
    AppScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.recording))
            Text(stringResource(if (keepDisplayOn) R.string.display_stays_on else R.string.display_may_sleep))
            Button(
                label = { Text(stringResource(R.string.stop_and_save)) },
                onClick = { accept(WearDashboardIntent.StopMeasurement) },
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.45f),
            )
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.WearSensorButton(
    sensor: WearSensorDescriptor,
    selected: Boolean,
    transformation: TransformationSpec,
    onClick: () -> Unit,
) = WearChoiceButton(sensor.name, selected, transformation, sensor.vendor, onClick)

@Composable
private fun TransformingLazyColumnItemScope.WearChoiceButton(
    label: String,
    selected: Boolean,
    transformation: TransformationSpec,
    detail: String? = null,
    onClick: () -> Unit,
) {
    Button(
        label = { Text(label) },
        secondaryLabel = detail?.let { { Text(it) } },
        icon = { Text(stringResource(if (selected) R.string.selected_symbol else R.string.unselected_symbol)) },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }
            .transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    )
}

@Composable
private fun TransformingLazyColumnItemScope.WearActionButton(
    label: String,
    transformation: TransformationSpec,
    onClick: () -> Unit,
) {
    Button(
        label = { Text(label) },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    )
}

@Composable
private fun TransformingLazyColumnItemScope.WearBackButton(
    transformation: TransformationSpec,
    accept: (WearDashboardIntent) -> Unit,
) = WearActionButton(stringResource(R.string.back), transformation) { accept(WearDashboardIntent.Back) }

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

@Composable
private fun TransformingLazyColumnItemScope.WearHeader(label: String, transformation: TransformationSpec) {
    ListHeader(
        modifier = Modifier.fillMaxWidth().transformedHeight(this, transformation),
        transformation = SurfaceTransformation(transformation),
    ) { Text(label) }
}
