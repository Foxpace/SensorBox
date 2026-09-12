package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.recording.preview.GpsPreviewData
import com.tomasrepcik.sensorbox.recording.preview.SensorPreviewData
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetup
import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor
import kotlinx.serialization.Serializable

enum class RecordingMessage {
    NONE,
    PICK_AT_LEAST_ONE_SOURCE,
    RECORDING_ARCHIVE_REQUIRED,
    PERMISSION_REQUIRED,
    WATCH_PERMISSION_REQUIRED,
    RECORDING_FAILED,
}

@Serializable
enum class RecordingDevice {
    PHONE,
    WATCH,
}

interface RecordingScreenEffect

data class RecordingState(
    val sensors: List<SensorDescriptor> = emptyList(),
    val watchSensors: List<SensorDescriptor> = emptyList(),
    val detailsSensorType: Int? = null,
    val detailsDevice: RecordingDevice = RecordingDevice.PHONE,
    val selectedSensorIds: Set<Int> = emptySet(),
    val includesGps: Boolean = false,
    val selectedWatchSensorIds: Set<Int> = emptySet(),
    val watchIncludesGps: Boolean = false,
    val customMeasurementName: String = "",
    val startDelaySeconds: Int = 0,
    val durationSeconds: Int = 0,
    val notes: String = "",
    val alarmOffsets: String = "",
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
    val recordingArchivePath: String? = null,
    val preferences: AppPreferences = AppPreferences(),
    val session: RecordingSessionState = RecordingSessionState.Idle,
    val elapsedSeconds: Long = 0,
    val isWatchConnected: Boolean = false,
    val isStarting: Boolean = false,
    val isSyncingWatch: Boolean = false,
    val startCountdownSeconds: Int? = null,
    val message: RecordingMessage = RecordingMessage.NONE,
    val errorCode: AppErrorCode? = null,
    val gpsPreview: GpsPreviewData = GpsPreviewData(hasPermission = false),
    val sensorPreview: SensorPreviewData = SensorPreviewData(isAvailable = true),
) {
    fun toRecordingSetup() = RecordingSetup(
        sensorIds = selectedSensorIds,
        includesGps = includesGps,
        samplingPeriodIndex = preferences.recording.sensorSamplingPeriod,
        stopOnLowBattery = preferences.recording.stopRecordingOnLowBattery,
        useWakeLock = preferences.recording.useWakeLock,
        gpsIntervalSeconds = preferences.recording.gpsIntervalSeconds,
        gpsMinDistanceMeters = preferences.recording.gpsMinDistanceMeters,
        watchSensorIds = selectedWatchSensorIds,
        watchIncludesGps = watchIncludesGps,
        customName = customMeasurementName,
        durationSeconds = durationSeconds.coerceAtLeast(0),
        notes = notes.lines().map(String::trim).filter(String::isNotEmpty),
        alarmOffsetsSeconds = alarmOffsets.split(',', ';', ' ')
            .mapNotNull(String::toIntOrNull).filter { it >= 0 },
        activityRecognition = activityRecognition,
        activityRecognitionPeriodSeconds = activityRecognitionPeriodSeconds,
        significantMotion = significantMotion,
    )
}
