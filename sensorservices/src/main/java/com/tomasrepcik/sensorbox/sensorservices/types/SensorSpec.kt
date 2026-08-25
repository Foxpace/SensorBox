package com.tomasrepcik.sensorbox.sensorservices.types

import android.hardware.Sensor

enum class SensorSpec(val type: Int, val axisCount: Int, val fileName: String, val header: String) {
    ACCELEROMETER(Sensor.TYPE_ACCELEROMETER, 3, "accelerometer.csv", "t_sensor;t_unix;x;y;z;accuracy\n"),
    AMBIENT_TEMPERATURE(
        Sensor.TYPE_AMBIENT_TEMPERATURE,
        1,
        "ambient_temperature.csv",
        "t_sensor;t_unix;value;accuracy\n",
    ),
    GRAVITY(Sensor.TYPE_GRAVITY, 3, "gravity.csv", "t_sensor;t_unix;x;y;z;accuracy\n"),
    GYROSCOPE(Sensor.TYPE_GYROSCOPE, 3, "gyroscope.csv", "t_sensor;t_unix;x;y;z;accuracy\n"),
    LIGHT(Sensor.TYPE_LIGHT, 1, "light.csv", "t_sensor;t_unix;value;accuracy\n"),
    LINEAR_ACCELERATION(
        Sensor.TYPE_LINEAR_ACCELERATION,
        3,
        "linear_acceleration.csv",
        "t_sensor;t_unix;x;y;z;accuracy\n",
    ),
    MAGNETIC_FIELD(Sensor.TYPE_MAGNETIC_FIELD, 3, "magnetic_field.csv", "t_sensor;t_unix;x;y;z;accuracy\n"),
    PRESSURE(Sensor.TYPE_PRESSURE, 1, "pressure.csv", "t_sensor;t_unix;value;accuracy\n"),
    PROXIMITY(Sensor.TYPE_PROXIMITY, 1, "proximity.csv", "t_sensor;t_unix;value;accuracy\n"),
    RELATIVE_HUMIDITY(
        Sensor.TYPE_RELATIVE_HUMIDITY,
        1,
        "relative_humidity.csv",
        "t_sensor;t_unix;value;accuracy\n",
    ),
    ROTATION_VECTOR(Sensor.TYPE_ROTATION_VECTOR, 4, "rotation_vector.csv", "t_sensor;t_unix;x;y;z;scalar;accuracy\n"),
    STEP_COUNTER(Sensor.TYPE_STEP_COUNTER, 1, "step_counter.csv", "t_sensor;t_unix;steps;accuracy\n"),
    STEP_DETECTOR(Sensor.TYPE_STEP_DETECTOR, 1, "step_detector.csv", "t_sensor;t_unix;step;accuracy\n"),

    // Kept discoverable in the sensor catalogue, but recorded by SignificantMotion's trigger listener.
    SIGNIFICANT_MOTION(
        Sensor.TYPE_SIGNIFICANT_MOTION,
        1,
        "significant_motion.csv",
        "t_sensor;t_unix;event;accuracy\n",
    ),
    ;

    companion object {
        fun fromType(type: Int): SensorSpec? = entries.firstOrNull { it.type == type }
    }
}
