package com.motionapps.sensorservices.intent

data class MeasurementLaunchRequest(
    val folderName: String,
    val useInternalStorage: Boolean,
    val sensorIds: Set<Int>,
    val sensorSamplingPeriod: Int,
    val includesGps: Boolean,
    val stopOnLowBattery: Boolean,
    val useWakeLock: Boolean,
    val gpsIntervalSeconds: Int,
    val gpsMinDistanceMeters: Int,
    val measurementType: String = "ENDLESS",
    val startAtEpochMillis: Long = System.currentTimeMillis(),
    val durationMillis: Long = 0L,
    val notes: List<String> = emptyList(),
    val alarmOffsetsSeconds: List<Int> = emptyList(),
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
    val controlsWearMeasurement: Boolean = false,
)
