package com.motionapps.sensorbox.domain.measurement

import android.hardware.SensorManager
import com.motionapps.sensorservices.intent.MeasurementLaunchRequest

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
) {
    fun toLaunchRequest(sessionId: String, folderName: String) = MeasurementLaunchRequest(
        sessionId = sessionId,
        folderName = folderName,
        useInternalStorage = false,
        sensorIds = sensorIds,
        sensorSamplingPeriod = SENSOR_PERIODS.getOrElse(samplingPeriodIndex) {
            SensorManager.SENSOR_DELAY_FASTEST
        },
        includesGps = includesGps,
        stopOnLowBattery = stopOnLowBattery,
        useWakeLock = useWakeLock,
        gpsIntervalSeconds = gpsIntervalSeconds,
        gpsMinDistanceMeters = gpsMinDistanceMeters,
        measurementType = measurementType,
        durationMillis = durationSeconds.coerceAtLeast(0) * 1_000L,
        notes = notes,
        alarmOffsetsSeconds = alarmOffsetsSeconds,
        activityRecognition = activityRecognition,
        activityRecognitionPeriodSeconds = activityRecognitionPeriodSeconds,
        significantMotion = significantMotion,
    )

    private companion object {
        val SENSOR_PERIODS = intArrayOf(
            SensorManager.SENSOR_DELAY_FASTEST,
            SensorManager.SENSOR_DELAY_GAME,
            SensorManager.SENSOR_DELAY_UI,
            SensorManager.SENSOR_DELAY_NORMAL,
        )
    }
}
