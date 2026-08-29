package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.navigation.MainRoute
import com.tomasrepcik.sensorbox.recording.RecordingDevice
import com.tomasrepcik.sensorbox.recording.RecordingIntent
import com.tomasrepcik.sensorbox.recording.RecordingMessageText
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.setup.GpsRow
import com.tomasrepcik.sensorbox.recording.setup.RecordHeader
import com.tomasrepcik.sensorbox.recording.setup.SensorSourceItem
import com.tomasrepcik.sensorbox.recording.setup.SourceDivider
import com.tomasrepcik.sensorbox.recording.setup.WearSectionHeader
import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun RecordContent(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 24.dp)) {
        stickyHeader {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column {
                    RecordHeader(
                        onMeasurements = { onIntent(RecordingIntent.Navigate(MainRoute.MEASUREMENTS)) },
                        onOptions = { onIntent(RecordingIntent.Navigate(MainRoute.SETTINGS)) },
                    )
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
        phoneSourceItems(state, onIntent)
        if (state.isWatchConnected) watchSourceItems(state, onIntent)
        item { RecordingMessageText(state.message) }
    }
}

private fun LazyListScope.phoneSourceItems(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    item {
        GpsRow(
            selected = state.includesGps,
            onToggle = { onIntent(RecordingIntent.ToggleGps) },
            onInfo = { onIntent(RecordingIntent.OpenSensorDetails(null)) },
        )
    }
    item { SourceDivider() }
    items(state.sensors, key = SensorDescriptor::type) { sensor ->
        SensorSourceItem(
            sensor = sensor,
            selected = sensor.type in state.selectedSensorIds,
            onToggle = { onIntent(RecordingIntent.ToggleSensor(sensor.type)) },
            onInfo = { onIntent(RecordingIntent.OpenSensorDetails(sensor.type)) },
        )
    }
}

private fun LazyListScope.watchSourceItems(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    item { WearSectionHeader() }
    item {
        GpsRow(
            selected = state.watchIncludesGps,
            onToggle = { onIntent(RecordingIntent.ToggleWatchGps) },
        )
    }
    item { SourceDivider() }
    items(state.watchSensors, key = { "watch_${it.type}" }) { sensor ->
        SensorSourceItem(
            sensor = sensor,
            selected = sensor.type in state.selectedWatchSensorIds,
            onToggle = { onIntent(RecordingIntent.ToggleWatchSensor(sensor.type)) },
            onInfo = { onIntent(RecordingIntent.OpenSensorDetails(sensor.type, RecordingDevice.WATCH)) },
        )
    }
}
