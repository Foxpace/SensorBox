package com.tomasrepcik.sensorbox.domain.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

fun interface AvailableSensorsUseCase {
    operator fun invoke(): List<SensorDescriptor>
}

class GetAvailableSensorsUseCase @Inject constructor(@ApplicationContext context: Context) : AvailableSensorsUseCase {
    private val sensorManager = context.getSystemService(SensorManager::class.java)

    override fun invoke(): List<SensorDescriptor> = sensorManager
        .getSensorList(Sensor.TYPE_ALL)
        .filter { it.type in PHONE_SENSOR_TYPES }
        .distinctBy(Sensor::getType)
        .map { it.toDescriptor() }
        .sortedBy(SensorDescriptor::name)

    private fun Sensor.toDescriptor() = SensorDescriptor(
        type = type,
        name = name,
        vendor = vendor,
        version = version,
        stringType = stringType,
        maximumRange = maximumRange,
        resolution = resolution,
        power = power,
        minimumDelayMicros = minDelay,
        maximumDelayMicros = maxDelay,
        reportingMode = reportingMode.toSensorReportingMode(),
        isWakeUpSensor = isWakeUpSensor,
    )
}

internal val PHONE_SENSOR_TYPES = setOf(
    Sensor.TYPE_ACCELEROMETER,
    Sensor.TYPE_AMBIENT_TEMPERATURE,
    Sensor.TYPE_GRAVITY,
    Sensor.TYPE_GYROSCOPE,
    Sensor.TYPE_LIGHT,
    Sensor.TYPE_LINEAR_ACCELERATION,
    Sensor.TYPE_MAGNETIC_FIELD,
    Sensor.TYPE_PRESSURE,
    Sensor.TYPE_PROXIMITY,
    Sensor.TYPE_RELATIVE_HUMIDITY,
    Sensor.TYPE_ROTATION_VECTOR,
    Sensor.TYPE_STEP_COUNTER,
    Sensor.TYPE_STEP_DETECTOR,
)

internal fun Int.toSensorReportingMode(): SensorReportingMode = when (this) {
    Sensor.REPORTING_MODE_CONTINUOUS -> SensorReportingMode.CONTINUOUS
    Sensor.REPORTING_MODE_ON_CHANGE -> SensorReportingMode.ON_CHANGE
    Sensor.REPORTING_MODE_ONE_SHOT -> SensorReportingMode.ONE_SHOT
    Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> SensorReportingMode.SPECIAL_TRIGGER
    else -> SensorReportingMode.UNKNOWN
}
