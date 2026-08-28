package com.tomasrepcik.sensorbox.sensorservices.intent

import android.content.Intent
import android.hardware.SensorManager
import com.tomasrepcik.sensorbox.sensorservices.services.MeasurementService
import java.util.UUID

data class MeasurementLaunchRequest(
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
        fun from(intent: Intent): MeasurementLaunchRequest = MeasurementLaunchRequest(
            sessionId = intent.getStringExtra(MeasurementService.SESSION_ID).orEmpty(),
            folderName = intent.getStringExtra(MeasurementService.FOLDER_NAME).orEmpty(),
            useInternalStorage = intent.getBooleanExtra(MeasurementService.INTERNAL_STORAGE, false),
            sensorIds = intent.getIntArrayExtra(MeasurementService.ANDROID_SENSORS)?.toSet().orEmpty(),
            sensorSamplingPeriod = intent.getIntExtra(
                MeasurementService.ANDROID_SENSORS_SPEED,
                SensorManager.SENSOR_DELAY_FASTEST,
            ),
            includesGps = intent.getBooleanExtra(MeasurementService.GPS, false),
            stopOnLowBattery = intent.getBooleanExtra(MeasurementService.STOP_ON_LOW_BATTERY, false),
            useWakeLock = intent.getBooleanExtra(MeasurementService.USE_WAKE_LOCK, false),
            gpsIntervalSeconds = intent.getIntExtra(MeasurementService.GPS_INTERVAL_SECONDS, 10),
            gpsMinDistanceMeters = intent.getIntExtra(MeasurementService.GPS_DISTANCE_METERS, 20),
            durationMillis = intent.getLongExtra(MeasurementService.DURATION_MILLIS, 0L).coerceAtLeast(0L),
            notes = intent.getStringArrayListExtra(MeasurementService.NOTES).orEmpty(),
            alarmOffsetsSeconds = intent.getIntArrayExtra(MeasurementService.ALARM_OFFSETS_SECONDS)
                ?.filter { it >= 0 }.orEmpty(),
            activityRecognition = intent.getBooleanExtra(MeasurementService.ACTIVITY_RECOGNITION, false),
            activityRecognitionPeriodSeconds = intent.getIntExtra(
                MeasurementService.ACTIVITY_RECOGNITION_PERIOD_SECONDS,
                30,
            ).coerceAtLeast(1),
            significantMotion = intent.getBooleanExtra(MeasurementService.SIGNIFICANT_MOTION, false),
        )
    }
}
