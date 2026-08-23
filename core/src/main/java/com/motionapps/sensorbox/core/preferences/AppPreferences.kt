package com.motionapps.sensorbox.core.preferences

data class AppPreferences(
    val onboarding: OnboardingPreferences = OnboardingPreferences(),
    val recording: RecordingPreferences = RecordingPreferences(),
    val display: DisplayPreferences = DisplayPreferences(),
)

data class OnboardingPreferences(val hasCompletedIntro: Boolean = false, val hasAcceptedPolicy: Boolean = false)

data class RecordingPreferences(
    val gpsIntervalSeconds: Int = 10,
    val gpsMinDistanceMeters: Int = 20,
    val sensorSamplingPeriod: Int = 0,
    val restrictMeasurementOnLowBattery: Boolean = true,
    val useWakeLock: Boolean = false,
)

data class DisplayPreferences(val keepPhoneDisplayOn: Boolean = false, val keepWearDisplayOn: Boolean = false)
