package com.tomasrepcik.sensorbox.presentation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor

@Composable
fun RecordScreen(state: RecordingState, onIntent: (RecordingIntent) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        RecordContent(state, onIntent)
        SensorSelectionActionBar(state, onIntent, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RecordContent(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 112.dp),
    ) {
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column {
                    RecordHeader { onIntent(RecordingIntent.Navigate(MainRoute.SETTINGS)) }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
        item {
            GpsRow(
                selected = state.includesGps,
                onToggle = { onIntent(RecordingIntent.ToggleGps) },
                onInfo = { onIntent(RecordingIntent.OpenSensorDetails(null)) },
            )
        }
        item { SourceDivider() }
        items(state.sensors, key = SensorDescriptor::type) { sensor ->
            Column {
                SensorRow(
                    sensor = sensor,
                    selected = sensor.type in state.selectedSensorIds,
                    onToggle = { onIntent(RecordingIntent.ToggleSensor(sensor.type)) },
                    onInfo = { onIntent(RecordingIntent.OpenSensorDetails(sensor.type)) },
                )
                SourceDivider()
            }
        }
        if (state.isWearConnected) {
            item { WearSectionHeader() }
            item {
                GpsRow(
                    selected = state.wearIncludesGps,
                    onToggle = { onIntent(RecordingIntent.ToggleWearGps) },
                    onInfo = { onIntent(RecordingIntent.OpenSensorDetails(null)) },
                )
            }
            item { SourceDivider() }
            items(state.wearSensors, key = { "wear_${it.type}" }) { sensor ->
                Column {
                    SensorRow(
                        sensor = sensor,
                        selected = sensor.type in state.selectedWearSensorIds,
                        onToggle = { onIntent(RecordingIntent.ToggleWearSensor(sensor.type)) },
                        onInfo = { onIntent(RecordingIntent.OpenSensorDetails(sensor.type)) },
                    )
                    SourceDivider()
                }
            }
        }
        item { RecordingMessageText(state.message) }
    }
}

@Composable
private fun WearSectionHeader() {
    Text(
        stringResource(R.string.wear_sensors),
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun GpsRow(selected: Boolean, onToggle: () -> Unit, onInfo: () -> Unit) {
    SourceRow(
        title = stringResource(R.string.gps),
        icon = R.drawable.ic_source_location,
        selected = selected,
        informationDescription = stringResource(R.string.information_about_gps),
        onToggle = onToggle,
        onInfo = onInfo,
    )
}

@Composable
private fun RecordHeader(onOptions: () -> Unit) {
    SensorBoxTopAppBar(
        title = stringResource(R.string.sources),
        actions = {
            IconButton(onClick = onOptions) {
                Icon(
                    painterResource(R.drawable.ic_baseline_settings_24),
                    contentDescription = stringResource(R.string.options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun SensorRow(sensor: SensorDescriptor, selected: Boolean, onToggle: () -> Unit, onInfo: () -> Unit) {
    SourceRow(
        title = sensor.name,
        icon = sensorIconResource(sensor.type),
        selected = selected,
        informationDescription = stringResource(R.string.information_about_sensor, sensor.name),
        onToggle = onToggle,
        onInfo = onInfo,
    )
}

@Composable
private fun SourceRow(
    title: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    informationDescription: String,
    onToggle: () -> Unit,
    onInfo: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(18.dp))
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onInfo) {
            Icon(
                painterResource(R.drawable.ic_info),
                contentDescription = informationDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(4.dp))
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SourceDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
}

@Composable
private fun SensorSelectionActionBar(
    state: RecordingState,
    onIntent: (RecordingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sensorCount = selectedSourceCount(state)
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            SensorBoxPrimaryButton(
                label = stringResource(R.string.continue_action),
                onClick = { onIntent(RecordingIntent.OpenMeasurementSetup) },
                modifier = Modifier.widthIn(min = 176.dp, max = 240.dp),
                enabled = sensorCount > 0,
            )
        }
    }
}

private fun selectedSourceCount(state: RecordingState): Int = state.selectedSensorIds.size +
    state.selectedWearSensorIds.size +
    (if (state.includesGps) 1 else 0) +
    (if (state.wearIncludesGps) 1 else 0) +
    (if (state.activityRecognition) 1 else 0) +
    (if (state.significantMotion) 1 else 0)

@Composable
fun RecordingMessageText(message: RecordingMessage) {
    if (message == RecordingMessage.NONE) return
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
        Text(
            stringResource(messageTextResource(message)),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(14.dp),
        )
    }
}

@StringRes
private fun messageTextResource(message: RecordingMessage): Int = when (message) {
    RecordingMessage.PICK_AT_LEAST_ONE_SOURCE -> R.string.message_pick_source
    RecordingMessage.STORAGE_REQUIRED -> R.string.message_storage_required
    RecordingMessage.PERMISSION_REQUIRED -> R.string.message_permission_required
    RecordingMessage.MEASUREMENT_FAILED -> R.string.message_measurement_failed
    RecordingMessage.NONE -> R.string.app_name
}
