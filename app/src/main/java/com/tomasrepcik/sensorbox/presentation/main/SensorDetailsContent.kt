package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor

internal fun selectedDetailsSensor(state: RecordingState): SensorDescriptor? {
    val sensorType = state.detailsSensorType ?: return null
    val sensors = when (state.detailsDevice) {
        RecordingDevice.PHONE -> state.sensors
        RecordingDevice.WATCH -> state.watchSensors
    }
    return sensors.firstOrNull { it.type == sensorType }
}

@Composable
internal fun SensorDetailsList(
    state: RecordingState,
    sensor: SensorDescriptor?,
    title: String,
    showPreview: Boolean,
    onIntent: (RecordingIntent) -> Unit,
    onBack: () -> Unit,
) {
    SensorBoxBackScreen(
        title = title,
        onBack = onBack,
        bottomPadding = if (showPreview) 104.dp else 24.dp,
    ) {
        if (state.detailsSensorType == null) {
            item { GpsDetails(state, onIntent) }
        } else if (sensor != null) {
            item { HardwareSensorDetails(sensor) }
        }
    }
}

@Composable
internal fun PreviewButton(onPreview: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            SensorBoxPrimaryButton(
                label = stringResource(R.string.preview),
                onClick = onPreview,
                modifier = Modifier.widthIn(min = 176.dp, max = 240.dp),
            )
        }
    }
}
