package com.tomasrepcik.sensorbox.domain.recording

import android.hardware.SensorManager
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingRequest

data class RecordingSetup(
    val sensorIds: Set<Int>,
    val includesGps: Boolean,
    val samplingPeriodIndex: Int,
    val stopOnLowBattery: Boolean,
    val useWakeLock: Boolean,
    val gpsIntervalSeconds: Int,
    val gpsMinDistanceMeters: Int,
    val watchSensorIds: Set<Int> = emptySet(),
    val watchIncludesGps: Boolean = false,
    val customName: String = "",
    val durationSeconds: Int = 0,
    val notes: List<String> = emptyList(),
    val alarmOffsetsSeconds: List<Int> = emptyList(),
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
) {
    fun toRecordingRequest(sessionId: String, folderName: String) = RecordingRequest(
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

data class StartedPhoneRecording(val sessionId: String, val folderName: String, val durationMillis: Long)

fun StartedPhoneRecording.toWatchRecordingRequest(request: RecordingSetup) = WearRecordingRequest(
    folderName = folderName,
    sensorIds = request.watchSensorIds.sorted(),
    includesGps = request.watchIncludesGps,
    durationMillis = durationMillis,
)
