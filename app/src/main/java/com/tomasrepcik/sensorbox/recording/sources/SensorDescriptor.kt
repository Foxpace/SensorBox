package com.tomasrepcik.sensorbox.recording.sources

import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearSensorInfo

data class SensorDescriptor(
    val type: Int,
    val name: String,
    val vendor: String,
    val version: Int = 0,
    val stringType: String = "",
    val maximumRange: Float = 0f,
    val resolution: Float = 0f,
    val power: Float = 0f,
    val minimumDelayMicros: Int = 0,
    val maximumDelayMicros: Int = 0,
    val reportingMode: SensorReportingMode = SensorReportingMode.UNKNOWN,
    val isWakeUpSensor: Boolean = false,
)

enum class SensorReportingMode {
    CONTINUOUS,
    ON_CHANGE,
    ONE_SHOT,
    SPECIAL_TRIGGER,
    UNKNOWN,
}

fun WearSensorInfo.toSensorDescriptor() = SensorDescriptor(
    type = type,
    name = name,
    vendor = vendor,
    version = version,
    stringType = stringType,
    maximumRange = maximumRange,
    resolution = resolution,
    power = power,
    minimumDelayMicros = minimumDelayMicros,
    maximumDelayMicros = maximumDelayMicros,
    reportingMode = reportingMode.toSensorReportingMode(),
    isWakeUpSensor = isWakeUpSensor,
)
