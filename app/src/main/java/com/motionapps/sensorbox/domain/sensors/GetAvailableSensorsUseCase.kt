package com.motionapps.sensorbox.domain.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GetAvailableSensorsUseCase @Inject constructor(@ApplicationContext context: Context) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)

    operator fun invoke(): List<SensorDescriptor> = sensorManager
        .getSensorList(Sensor.TYPE_ALL)
        .filter { it.type in SUPPORTED_SENSOR_TYPES }
        .distinctBy(Sensor::getType)
        .map { it.toDescriptor() }
        .sortedBy(SensorDescriptor::name)

    private fun Sensor.toDescriptor() = SensorDescriptor(
        type = type,
        name = name,
        vendor = vendor,
        isHeartRate = type == Sensor.TYPE_HEART_RATE,
        version = version,
        stringType = stringType,
        maximumRange = maximumRange,
        resolution = resolution,
        power = power,
        minimumDelayMicros = minDelay,
        maximumDelayMicros = maxDelay,
        reportingMode = reportingMode,
        isWakeUpSensor = isWakeUpSensor,
    )

    private companion object {
        val SUPPORTED_SENSOR_TYPES = setOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_HEART_RATE,
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
    }
}
