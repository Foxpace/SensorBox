package com.tomasrepcik.sensorbox.recording.details

import android.hardware.Sensor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.recording.RecordingDevice
import com.tomasrepcik.sensorbox.recording.RecordingState

@Composable
fun SensorDetailsScreen(
    state: RecordingState,
    onBack: () -> Unit,
    onPreview: () -> Unit,
    onIntent: (SensorDetailsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sensor = selectedDetailsSensor(state)
    val canPreview = state.detailsSensorType == null || sensor?.type != Sensor.TYPE_STEP_DETECTOR
    val title = when {
        state.detailsSensorType == null -> stringResource(R.string.gps)
        sensor != null -> sensor.name
        else -> stringResource(R.string.sensor_unavailable)
    }
    val showPreview = state.detailsDevice == RecordingDevice.PHONE &&
        (state.detailsSensorType == null || sensor != null) && canPreview
    Box(modifier.fillMaxSize()) {
        SensorDetailsList(state, sensor, title, showPreview, onIntent, onBack)
        if (showPreview) PreviewButton(onPreview, Modifier.align(Alignment.BottomCenter))
    }
}
