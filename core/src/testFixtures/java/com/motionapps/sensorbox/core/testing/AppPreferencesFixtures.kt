package com.motionapps.sensorbox.core.testing

import com.motionapps.sensorbox.core.preferences.AppPreferences

object AppPreferencesFixtures {
    fun preferences(gpsIntervalSeconds: Int = 10, gpsMinDistanceMeters: Int = 20): AppPreferences = AppPreferences(
        gpsIntervalSeconds = gpsIntervalSeconds,
        gpsMinDistanceMeters = gpsMinDistanceMeters,
    )
}
