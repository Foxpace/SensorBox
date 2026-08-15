package com.motionapps.sensorbox.domain.measurement

data class MeasurementRequest(
    val sensorIds: Set<Int>,
    val includesGps: Boolean,
    val samplingPeriodIndex: Int,
    val stopOnLowBattery: Boolean,
    val useWakeLock: Boolean,
    val gpsIntervalSeconds: Int,
    val gpsMinDistanceMeters: Int,
    val wearSensorIds: Set<Int> = emptySet(),
    val wearIncludesGps: Boolean = false,
    val customName: String = "",
    val measurementType: String = "ENDLESS",
    val delaySeconds: Int = 0,
    val durationSeconds: Int = 0,
    val notes: List<String> = emptyList(),
    val alarmOffsetsSeconds: List<Int> = emptyList(),
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
)
