package com.motionapps.sensorbox.domain.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GetWearSensorsUseCase @Inject constructor(@ApplicationContext context: Context) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)

    operator fun invoke(): List<WearSensorDescriptor> = sensorManager
        .getSensorList(Sensor.TYPE_ALL)
        .filter { it.type in WEAR_SENSOR_TYPES }
        .distinctBy(Sensor::getType)
        .map { sensor -> sensor.toDescriptor() }
        .sortedBy(WearSensorDescriptor::name)

    private fun Sensor.toDescriptor() = WearSensorDescriptor(
        type = type,
        name = name,
        vendor = vendor,
    )
}

internal val WEAR_SENSOR_TYPES = setOf(
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
