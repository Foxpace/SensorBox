package com.motionapps.sensorbox.core.testing

import com.motionapps.sensorbox.core.preferences.AppPreferences
import com.motionapps.sensorbox.core.preferences.RecordingPreferences

object AppPreferencesFixtures {
    fun preferences(gpsIntervalSeconds: Int = 10, gpsMinDistanceMeters: Int = 20): AppPreferences = AppPreferences(
        recording = RecordingPreferences(
            gpsIntervalSeconds = gpsIntervalSeconds,
            gpsMinDistanceMeters = gpsMinDistanceMeters,
        ),
    )
}
