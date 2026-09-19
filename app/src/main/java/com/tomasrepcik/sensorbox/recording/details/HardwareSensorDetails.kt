package com.tomasrepcik.sensorbox.recording.details

import android.hardware.Sensor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor
import com.tomasrepcik.sensorbox.recording.sources.SensorReportingMode

@Composable
internal fun HardwareSensorDetails(sensor: SensorDescriptor) {
    Column(Modifier.fillMaxWidth()) {
        val unit = sensorUnit(sensor.type)
        SensorIdentityDetails(sensor, unit)
        SensorTimingDetails(sensor)
        SensorBehaviorDetails(sensor)
    }
}

@Composable
private fun SensorIdentityDetails(sensor: SensorDescriptor, unit: String) {
    val type = sensor.stringType.ifBlank { stringResource(R.string.detail_sensor_type_value, sensor.type) }
    ParameterRow(stringResource(R.string.detail_version), sensor.version.toString())
    ParameterRow(stringResource(R.string.detail_vendor), sensor.vendor)
    ParameterRow(stringResource(R.string.detail_resolution, unit), sensor.resolution.toString())
    ParameterRow(stringResource(R.string.detail_power), formatDecimal(sensor.power))
    ParameterRow(stringResource(R.string.detail_maximum_range, unit), formatDecimal(sensor.maximumRange))
    ParameterRow(stringResource(R.string.detail_android_sensor_type), type)
}

@Composable
private fun SensorTimingDetails(sensor: SensorDescriptor) {
    ParameterRow(
        stringResource(R.string.detail_minimum_delay),
        stringResource(R.string.detail_delay_value, sensor.minimumDelayMicros),
    )
    ParameterRow(
        stringResource(R.string.detail_maximum_delay),
        stringResource(R.string.detail_delay_value, sensor.maximumDelayMicros),
    )
}

@Composable
private fun SensorBehaviorDetails(sensor: SensorDescriptor) {
    ParameterRow(stringResource(R.string.detail_reporting_mode), reportingModeLabel(sensor.reportingMode))
    ParameterRow(
        stringResource(R.string.detail_wakeup_sensor),
        stringResource(if (sensor.isWakeUpSensor) R.string.yes else R.string.no),
        showDivider = false,
    )
}

@Composable
private fun reportingModeLabel(mode: SensorReportingMode): String = when (mode) {
    SensorReportingMode.CONTINUOUS -> stringResource(R.string.reporting_continuous)
    SensorReportingMode.ON_CHANGE -> stringResource(R.string.reporting_on_change)
    SensorReportingMode.ONE_SHOT -> stringResource(R.string.reporting_one_shot)
    SensorReportingMode.SPECIAL_TRIGGER -> stringResource(R.string.reporting_special_trigger)
    SensorReportingMode.UNKNOWN -> stringResource(R.string.unknown)
}

@Composable
internal fun sensorUnit(type: Int): String = when (type) {
    Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GRAVITY, Sensor.TYPE_LINEAR_ACCELERATION ->
        stringResource(R.string.unit_acceleration)

    Sensor.TYPE_GYROSCOPE -> stringResource(R.string.unit_angular_velocity)

    Sensor.TYPE_RELATIVE_HUMIDITY -> stringResource(R.string.unit_percent)

    Sensor.TYPE_MAGNETIC_FIELD -> stringResource(R.string.unit_magnetic_field)

    Sensor.TYPE_PROXIMITY -> stringResource(R.string.unit_centimeters)

    Sensor.TYPE_PRESSURE -> stringResource(R.string.unit_pressure)

    Sensor.TYPE_LIGHT -> stringResource(R.string.unit_lux)

    Sensor.TYPE_AMBIENT_TEMPERATURE -> stringResource(R.string.unit_temperature)

    Sensor.TYPE_STEP_COUNTER, Sensor.TYPE_STEP_DETECTOR -> stringResource(R.string.unit_steps)

    else -> stringResource(R.string.unit_none)
}

internal fun formatDecimal(value: Number): String = ValueFormats.decimal(value)
