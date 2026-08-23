package com.motionapps.sensorbox.presentation.main

import android.hardware.Sensor
import androidx.annotation.DrawableRes
import com.motionapps.sensorbox.R

@DrawableRes
fun sensorIconResource(sensorType: Int): Int = SENSOR_ICON_RESOURCES[sensorType] ?: R.drawable.ic_acceleration_icon

private val SENSOR_ICON_RESOURCES = mapOf(
    Sensor.TYPE_ACCELEROMETER to R.drawable.ic_acceleration_icon,
    Sensor.TYPE_LINEAR_ACCELERATION to R.drawable.ic_linear_acceleration_icon,
    Sensor.TYPE_GRAVITY to R.drawable.ic_gravity_icon,
    Sensor.TYPE_GYROSCOPE to R.drawable.ic_gyroscope_icon,
    Sensor.TYPE_GYROSCOPE_UNCALIBRATED to R.drawable.ic_gyroscope_icon,
    Sensor.TYPE_RELATIVE_HUMIDITY to R.drawable.ic_water_drop,
    Sensor.TYPE_MAGNETIC_FIELD to R.drawable.ic_magnet,
    Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED to R.drawable.ic_magnet,
    Sensor.TYPE_PROXIMITY to R.drawable.ic_proximity,
    Sensor.TYPE_ROTATION_VECTOR to R.drawable.ic_rotation_icon,
    Sensor.TYPE_GAME_ROTATION_VECTOR to R.drawable.ic_rotation_icon,
    Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR to R.drawable.ic_rotation_icon,
    Sensor.TYPE_PRESSURE to R.drawable.ic_pressure,
    Sensor.TYPE_LIGHT to R.drawable.ic_light,
    Sensor.TYPE_AMBIENT_TEMPERATURE to R.drawable.ic_temperature,
    Sensor.TYPE_STEP_COUNTER to R.drawable.ic_steps,
    Sensor.TYPE_STEP_DETECTOR to R.drawable.ic_steps_detector,
)
