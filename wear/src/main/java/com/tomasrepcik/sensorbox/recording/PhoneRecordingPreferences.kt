package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingRequest

internal fun WearRecordingRequest.recordingPreferences(watchPreferences: AppPreferences): AppPreferences {
    val requested = settings
    return watchPreferences.copy(
        recording = watchPreferences.recording.copy(
            sensorSamplingPeriod = requested.samplingPeriodIndex,
            stopRecordingOnLowBattery = requested.stopOnLowBattery,
            useWakeLock = requested.useWakeLock,
            gpsIntervalSeconds = requested.gpsIntervalSeconds,
            gpsMinDistanceMeters = requested.gpsMinDistanceMeters,
        ),
    )
}
