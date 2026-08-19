package com.motionapps.sensorbox.presentation.main

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor

@Composable
fun RecordScreen(state: RecordingState, onIntent: (RecordingIntent) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        RecordContent(state, onIntent)
        SensorSelectionActionBar(state, onIntent, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun RecordContent(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { RecordHeader(state) { onIntent(RecordingIntent.Navigate(MainRoute.SETTINGS)) } }
        item { DeviceSectionHeader(stringResource(R.string.phone_sensors)) }
        item { SensorSectionHeader(phoneSourceCount(state), state.sensors.size + 1) }
        item {
            GpsRow(
                selected = state.includesGps,
                onToggle = { onIntent(RecordingIntent.ToggleGps) },
                onInfo = { onIntent(RecordingIntent.OpenSensorDetails(null)) },
            )
        }
        items(state.sensors, key = SensorDescriptor::type) { sensor ->
            SensorRow(
                sensor = sensor,
                selected = sensor.type in state.selectedSensorIds,
                onToggle = { onIntent(RecordingIntent.ToggleSensor(sensor.type)) },
                onInfo = { onIntent(RecordingIntent.OpenSensorDetails(sensor.type)) },
            )
        }
        if (state.isWearConnected) {
            item { DeviceSectionHeader(stringResource(R.string.wear_sensors)) }
            item { SensorSectionHeader(wearSourceCount(state), state.wearSensors.size + 1) }
            item {
                GpsRow(
                    selected = state.wearIncludesGps,
                    onToggle = { onIntent(RecordingIntent.ToggleWearGps) },
                    onInfo = { onIntent(RecordingIntent.OpenSensorDetails(null)) },
                )
            }
            items(state.wearSensors, key = { "wear_${it.type}" }) { sensor ->
                SensorRow(
                    sensor = sensor,
                    selected = sensor.type in state.selectedWearSensorIds,
                    onToggle = { onIntent(RecordingIntent.ToggleWearSensor(sensor.type)) },
                    onInfo = { onIntent(RecordingIntent.OpenSensorDetails(sensor.type)) },
                )
            }
        }
        item { RecordingMessageText(state.message) }
    }
}

@Composable
private fun DeviceSectionHeader(label: String) {
    Text(
        label,
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun GpsRow(selected: Boolean, onToggle: () -> Unit, onInfo: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, sensorBorderColor(selected)),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_gps), stringResource(R.string.gps), Modifier.size(54.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.gps), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.device_location), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SensorInformationButton(stringResource(R.string.information_about_gps), onInfo)
            Spacer(Modifier.width(12.dp))
            SensorSelectionIndicator(selected)
        }
    }
}

@Composable
private fun RecordHeader(state: RecordingState, onOptions: () -> Unit) {
    val optionsDescription = stringResource(R.string.options)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        SensorBoxScreenHeader(
            title = stringResource(R.string.pick_sensors),
            subtitle = stringResource(
                if (state.isWearConnected) R.string.phone_and_wear_ready else R.string.choose_measurement_signals,
            ),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Surface(
            onClick = onOptions,
            modifier = Modifier.size(48.dp).semantics { contentDescription = optionsDescription },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_baseline_settings_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SensorSectionHeader(selected: Int, available: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.sensors), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.selected_count, selected, available),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SensorRow(sensor: SensorDescriptor, selected: Boolean, onToggle: () -> Unit, onInfo: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, sensorBorderColor(selected)),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            SensorIdentity(sensor)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(sensor.name, style = MaterialTheme.typography.titleMedium)
                Text(sensor.vendor, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SensorInformationButton(stringResource(R.string.information_about_sensor, sensor.name), onInfo)
            Spacer(Modifier.width(12.dp))
            SensorSelectionIndicator(selected)
        }
    }
}

@Composable
private fun SensorInformationButton(contentDescription: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.info_symbol),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { this.contentDescription = contentDescription },
            )
        }
    }
}

@Composable
private fun sensorBorderColor(selected: Boolean) =
    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

@Composable
private fun SensorSelectionActionBar(
    state: RecordingState,
    onIntent: (RecordingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sensorCount = selectedSourceCount(state)
    SensorBoxBottomAction(
        title = pluralStringResource(R.plurals.sensor_count, sensorCount, sensorCount),
        description = stringResource(R.string.step_one_of_two),
        buttonLabel = stringResource(R.string.continue_action),
        enabled = sensorCount > 0,
        onClick = { onIntent(RecordingIntent.OpenMeasurementSetup) },
        modifier = modifier,
    )
}

private fun phoneSourceCount(state: RecordingState): Int =
    state.selectedSensorIds.size + if (state.includesGps) 1 else 0

private fun wearSourceCount(state: RecordingState): Int =
    state.selectedWearSensorIds.size + if (state.wearIncludesGps) 1 else 0

private fun selectedSourceCount(state: RecordingState): Int = phoneSourceCount(state) + wearSourceCount(state) +
    (if (state.activityRecognition) 1 else 0) + (if (state.significantMotion) 1 else 0)

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
