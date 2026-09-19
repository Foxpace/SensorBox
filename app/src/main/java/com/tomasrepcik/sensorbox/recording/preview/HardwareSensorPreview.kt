package com.tomasrepcik.sensorbox.recording.preview

import android.hardware.Sensor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.recording.details.formatDecimal
import com.tomasrepcik.sensorbox.recording.details.sensorUnit
import com.tomasrepcik.sensorbox.recording.preview.SensorPreviewData
import com.tomasrepcik.sensorbox.recording.preview.SensorPreviewSample
import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor

@Composable
internal fun HardwareSensorPreview(sensor: SensorDescriptor, preview: SensorPreviewData) {
    val latestValues = preview.samples.lastOrNull()?.values
    val unit = sensorUnit(sensor.type)
    Column(Modifier.fillMaxWidth()) {
        if (!preview.isAvailable) {
            Text(stringResource(R.string.sensor_activation_failed), color = MaterialTheme.colorScheme.error)
            return@Column
        }
        if (sensor.type == Sensor.TYPE_STEP_COUNTER) {
            StepCounterPreview(latestValues?.firstOrNull())
        } else {
            SensorValues(latestValues, unit)
            PreviewSectionTitle(stringResource(R.string.live_chart))
            LiveSensorChart(preview.samples, unit)
        }
    }
}

@Composable
private fun StepCounterPreview(value: Float?) {
    if (value == null) {
        Text(stringResource(R.string.waiting_sensor_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value.toLong().toString(), style = MaterialTheme.typography.displayMedium)
        Text(stringResource(R.string.unit_steps), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SensorValues(values: List<Float>?, unit: String) {
    if (values == null) {
        Text(stringResource(R.string.waiting_sensor_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    values.forEachIndexed { index, value ->
        PreviewValueRow(
            label = stringResource(R.string.sensor_value_with_unit, axisLabel(index, values.size), unit),
            value = formatDecimal(value),
            showDivider = index != values.lastIndex,
        )
    }
}

@Composable
internal fun axisLabel(index: Int, axisCount: Int): String = when {
    axisCount == 1 -> stringResource(R.string.value)
    index == 0 -> stringResource(R.string.axis_x)
    index == 1 -> stringResource(R.string.axis_y)
    index == 2 -> stringResource(R.string.axis_z)
    index == 3 -> stringResource(R.string.axis_w)
    else -> stringResource(R.string.value_number, index + 1)
}

internal typealias TimedSensorSample = SensorPreviewSample
