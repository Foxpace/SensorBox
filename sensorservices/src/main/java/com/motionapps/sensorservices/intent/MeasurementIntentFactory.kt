package com.motionapps.sensorservices.intent

import android.content.Context
import android.content.Intent
import com.motionapps.sensorbox.core.time.ClockFormats
import com.motionapps.sensorbox.core.time.EpochClock
import com.motionapps.sensorservices.services.MeasurementService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MeasurementIntentFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: EpochClock,
) {
    fun create(request: MeasurementLaunchRequest): Intent = Intent(context, MeasurementService::class.java).apply {
        putExtra(MeasurementService.SESSION_ID, request.sessionId)
        putExtra(MeasurementService.FOLDER_NAME, request.folderName)
        putExtra(MeasurementService.INTERNAL_STORAGE, request.useInternalStorage)
        putExtra(MeasurementService.ANDROID_SENSORS, request.sensorIds.toIntArray())
        putExtra(MeasurementService.ANDROID_SENSORS_SPEED, request.sensorSamplingPeriod)
        putExtra(MeasurementService.GPS, request.includesGps)
        putExtra(MeasurementService.STOP_ON_LOW_BATTERY, request.stopOnLowBattery)
        putExtra(MeasurementService.USE_WAKE_LOCK, request.useWakeLock)
        putExtra(MeasurementService.GPS_INTERVAL_SECONDS, request.gpsIntervalSeconds)
        putExtra(MeasurementService.GPS_DISTANCE_METERS, request.gpsMinDistanceMeters)
        putExtra(MeasurementService.START_AT_EPOCH_MILLIS, request.startAtEpochMillis)
        putExtra(MeasurementService.DURATION_MILLIS, request.durationMillis)
        putStringArrayListExtra(MeasurementService.NOTES, ArrayList(request.notes))
        putExtra(MeasurementService.ALARM_OFFSETS_SECONDS, request.alarmOffsetsSeconds.toIntArray())
        putExtra(MeasurementService.ACTIVITY_RECOGNITION, request.activityRecognition)
        putExtra(MeasurementService.ACTIVITY_RECOGNITION_PERIOD_SECONDS, request.activityRecognitionPeriodSeconds)
        putExtra(MeasurementService.SIGNIFICANT_MOTION, request.significantMotion)
    }

    fun newFolderName(customName: String = ""): String {
        val prefix = customName.trim().replace(INVALID_NAME_CHARS, "_").trim('_').take(MAX_PREFIX_LENGTH)
            .ifBlank { "recording" }
        val timestamp = ClockFormats.folderTimestamp(clock.nowMillis())
        return "${prefix}_$timestamp"
    }

    private companion object {
        const val MAX_PREFIX_LENGTH = 60
        val INVALID_NAME_CHARS = Regex("[^A-Za-z0-9._-]+")
    }
}
