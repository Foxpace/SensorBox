package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R

@Composable
fun SensorPreviewScreen(
    state: RecordingState,
    onIntent: (RecordingIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.detailsDevice == RecordingDevice.WATCH) {
        SensorBoxBackScreen(
            title = stringResource(R.string.sensor_unavailable),
            onBack = onBack,
            modifier = modifier,
        ) {}
        return
    }
    val sensor = state.detailsSensorType?.let { type -> state.sensors.firstOrNull { it.type == type } }
    val title = when {
        state.detailsSensorType == null -> stringResource(R.string.gps)
        sensor != null -> sensor.name
        else -> stringResource(R.string.sensor_unavailable)
    }
    SensorBoxBackScreen(title = title, onBack = onBack, modifier = modifier) {
        if (state.detailsSensorType == null) {
            item { GpsPreview(state, onIntent) }
        } else if (sensor != null) {
            item {
                DisposableEffect(sensor.type) {
                    onIntent(RecordingIntent.StartSensorPreview(sensor.type))
                    onDispose { onIntent(RecordingIntent.StopPreview) }
                }
                HardwareSensorPreview(sensor, state.sensorPreview)
            }
        }
    }
}
