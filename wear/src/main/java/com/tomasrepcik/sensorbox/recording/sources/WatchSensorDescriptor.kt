package com.tomasrepcik.sensorbox.recording.sources

import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearSensorInfo

data class WatchSensorDescriptor(
    val type: Int,
    val name: String,
    val vendor: String,
    val version: Int,
    val stringType: String,
    val maximumRange: Float,
    val resolution: Float,
    val power: Float,
    val minimumDelayMicros: Int,
    val maximumDelayMicros: Int,
    val reportingMode: Int,
    val isWakeUpSensor: Boolean,
)

fun WatchSensorDescriptor.toWatchSensorInfo() = WearSensorInfo(
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
    reportingMode = reportingMode,
    isWakeUpSensor = isWakeUpSensor,
)
