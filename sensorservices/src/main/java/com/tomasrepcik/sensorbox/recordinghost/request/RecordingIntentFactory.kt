package com.tomasrepcik.sensorbox.recordinghost.request

import android.content.Context
import android.content.Intent
import com.tomasrepcik.sensorbox.core.format.ClockFormats
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recordinghost.session.RecordingService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RecordingIntentFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: EpochClock,
) {
    fun create(request: RecordingRequest): Intent = Intent(context, RecordingService::class.java).apply {
        putExtra(RecordingService.SESSION_ID, request.sessionId)
        putExtra(RecordingService.FOLDER_NAME, request.folderName)
        putExtra(RecordingService.INTERNAL_STORAGE, request.useInternalStorage)
        putExtra(RecordingService.ANDROID_SENSORS, request.sensorIds.toIntArray())
        putExtra(RecordingService.ANDROID_SENSORS_SPEED, request.sensorSamplingPeriod)
        putExtra(RecordingService.GPS, request.includesGps)
        putExtra(RecordingService.STOP_ON_LOW_BATTERY, request.stopOnLowBattery)
        putExtra(RecordingService.USE_WAKE_LOCK, request.useWakeLock)
        putExtra(RecordingService.GPS_INTERVAL_SECONDS, request.gpsIntervalSeconds)
        putExtra(RecordingService.GPS_DISTANCE_METERS, request.gpsMinDistanceMeters)
        putExtra(RecordingService.DURATION_MILLIS, request.durationMillis)
        putStringArrayListExtra(RecordingService.NOTES, ArrayList(request.notes))
        putExtra(RecordingService.ALARM_OFFSETS_SECONDS, request.alarmOffsetsSeconds.toIntArray())
        putExtra(RecordingService.ACTIVITY_RECOGNITION, request.activityRecognition)
        putExtra(RecordingService.ACTIVITY_RECOGNITION_PERIOD_SECONDS, request.activityRecognitionPeriodSeconds)
        putExtra(RecordingService.SIGNIFICANT_MOTION, request.significantMotion)
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
