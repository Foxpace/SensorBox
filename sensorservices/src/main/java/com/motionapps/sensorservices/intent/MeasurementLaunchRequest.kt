package com.motionapps.sensorservices.intent

import com.motionapps.sensorbox.core.time.SystemEpochClock

data class MeasurementLaunchRequest(
    val sessionId: String = java.util.UUID.randomUUID().toString(),
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
    val startAtEpochMillis: Long = SystemEpochClock.nowMillis(),
    val durationMillis: Long = 0L,
    val notes: List<String> = emptyList(),
    val alarmOffsetsSeconds: List<Int> = emptyList(),
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
)
