package com.motionapps.sensorbox.domain.sensors

data class SensorDescriptor(
    val type: Int,
    val name: String,
    val vendor: String,
    val isHeartRate: Boolean,
    val version: Int = 0,
    val stringType: String = "",
    val maximumRange: Float = 0f,
    val resolution: Float = 0f,
    val power: Float = 0f,
    val minimumDelayMicros: Int = 0,
    val maximumDelayMicros: Int = 0,
    val reportingMode: Int = 0,
    val isWakeUpSensor: Boolean = false,
)
