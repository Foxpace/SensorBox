package com.tomasrepcik.sensorbox.recordinghost.request

import android.content.Intent
import android.hardware.SensorManager
import com.tomasrepcik.sensorbox.recordinghost.session.RecordingService
import java.util.UUID

data class RecordingRequest(
    val sessionId: String = UUID.randomUUID().toString(),
    val folderName: String,
    val useInternalStorage: Boolean,
    val sensorIds: Set<Int>,
    val sensorSamplingPeriod: Int,
    val includesGps: Boolean,
    val stopOnLowBattery: Boolean,
    val useWakeLock: Boolean,
    val gpsIntervalSeconds: Int,
    val gpsMinDistanceMeters: Int,
    val durationMillis: Long = 0L,
    val notes: List<String> = emptyList(),
    val alarmOffsetsSeconds: List<Int> = emptyList(),
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
) {
    val requiresWakeLock: Boolean
        get() = useWakeLock || sensorIds.isNotEmpty()

    companion object {
        fun from(intent: Intent): RecordingRequest = RecordingRequest(
            sessionId = intent.getStringExtra(RecordingService.SESSION_ID).orEmpty(),
            folderName = intent.getStringExtra(RecordingService.FOLDER_NAME).orEmpty(),
            useInternalStorage = intent.getBooleanExtra(RecordingService.INTERNAL_STORAGE, false),
            sensorIds = intent.getIntArrayExtra(RecordingService.ANDROID_SENSORS)?.toSet().orEmpty(),
            sensorSamplingPeriod = intent.getIntExtra(
                RecordingService.ANDROID_SENSORS_SPEED,
                SensorManager.SENSOR_DELAY_FASTEST,
            ),
            includesGps = intent.getBooleanExtra(RecordingService.GPS, false),
            stopOnLowBattery = intent.getBooleanExtra(RecordingService.STOP_ON_LOW_BATTERY, false),
            useWakeLock = intent.getBooleanExtra(RecordingService.USE_WAKE_LOCK, false),
            gpsIntervalSeconds = intent.getIntExtra(RecordingService.GPS_INTERVAL_SECONDS, 10),
            gpsMinDistanceMeters = intent.getIntExtra(RecordingService.GPS_DISTANCE_METERS, 20),
            durationMillis = intent.getLongExtra(RecordingService.DURATION_MILLIS, 0L).coerceAtLeast(0L),
            notes = intent.getStringArrayListExtra(RecordingService.NOTES).orEmpty(),
            alarmOffsetsSeconds = intent.getIntArrayExtra(RecordingService.ALARM_OFFSETS_SECONDS)
                ?.filter { it >= 0 }.orEmpty(),
            activityRecognition = intent.getBooleanExtra(RecordingService.ACTIVITY_RECOGNITION, false),
            activityRecognitionPeriodSeconds = intent.getIntExtra(
                RecordingService.ACTIVITY_RECOGNITION_PERIOD_SECONDS,
                30,
            ).coerceAtLeast(1),
            significantMotion = intent.getBooleanExtra(RecordingService.SIGNIFICANT_MOTION, false),
        )
    }
}
