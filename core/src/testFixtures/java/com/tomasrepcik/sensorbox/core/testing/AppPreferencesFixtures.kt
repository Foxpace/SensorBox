package com.tomasrepcik.sensorbox.core.testing

import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.core.preferences.RecordingPreferences

object AppPreferencesFixtures {
    fun preferences(gpsIntervalSeconds: Int = 10, gpsMinDistanceMeters: Int = 20): AppPreferences = AppPreferences(
        recording = RecordingPreferences(
            gpsIntervalSeconds = gpsIntervalSeconds,
            gpsMinDistanceMeters = gpsMinDistanceMeters,
        ),
    )
}
