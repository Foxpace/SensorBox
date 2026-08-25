package com.tomasrepcik.sensorbox.sensorservices.serviceController

import android.content.Intent
import android.hardware.SensorManager
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.sensorservices.services.MeasurementService

data class MeasurementConfig(
    val sessionId: String,
    val folderName: String,
    val useInternalStorage: Boolean,
    val sensorIds: IntArray,
    val sensorSamplingPeriod: Int,
    val includesGps: Boolean,
    val stopOnLowBattery: Boolean,
    val useWakeLock: Boolean,
    val gpsIntervalSeconds: Int,
    val gpsMinDistanceMeters: Int,
    val startAtEpochMillis: Long,
    val durationMillis: Long,
    val notes: List<String>,
    val alarmOffsetsSeconds: List<Int>,
    val activityRecognition: Boolean,
    val activityRecognitionPeriodSeconds: Int,
    val significantMotion: Boolean,
) {
    companion object {
        fun from(intent: Intent, clock: EpochClock): MeasurementConfig = MeasurementConfig(
            sessionId = intent.getStringExtra(MeasurementService.SESSION_ID).orEmpty(),
            folderName = intent.getStringExtra(MeasurementService.FOLDER_NAME).orEmpty(),
            useInternalStorage = intent.getBooleanExtra(MeasurementService.INTERNAL_STORAGE, false),
            sensorIds = intent.getIntArrayExtra(MeasurementService.ANDROID_SENSORS) ?: intArrayOf(),
            sensorSamplingPeriod = intent.getIntExtra(
                MeasurementService.ANDROID_SENSORS_SPEED,
                SensorManager.SENSOR_DELAY_FASTEST,
            ),
            includesGps = intent.getBooleanExtra(MeasurementService.GPS, false),
            stopOnLowBattery = intent.getBooleanExtra(MeasurementService.STOP_ON_LOW_BATTERY, false),
            useWakeLock = intent.getBooleanExtra(MeasurementService.USE_WAKE_LOCK, false),
            gpsIntervalSeconds = intent.getIntExtra(MeasurementService.GPS_INTERVAL_SECONDS, 10),
            gpsMinDistanceMeters = intent.getIntExtra(MeasurementService.GPS_DISTANCE_METERS, 20),
            startAtEpochMillis = intent.getLongExtra(
                MeasurementService.START_AT_EPOCH_MILLIS,
                clock.nowMillis(),
            ),
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
