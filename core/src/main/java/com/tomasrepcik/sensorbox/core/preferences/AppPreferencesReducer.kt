package com.tomasrepcik.sensorbox.core.preferences

object AppPreferencesReducer {
    fun reduce(current: AppPreferences, intent: AppPreferencesIntent): AppPreferences = when (intent) {
        AppPreferencesIntent.AcceptPolicy -> current.copy(
            onboarding = current.onboarding.copy(hasAcceptedPolicy = true),
        )

        AppPreferencesIntent.CompleteIntro -> current.copy(
            onboarding = current.onboarding.copy(hasCompletedIntro = true),
        )

        is AppPreferencesIntent.SetGpsInterval -> current.copy(
            recording = current.recording.copy(gpsIntervalSeconds = intent.seconds.coerceAtLeast(1)),
        )

        is AppPreferencesIntent.SetGpsMinDistance -> current.copy(
            recording = current.recording.copy(gpsMinDistanceMeters = intent.meters.coerceAtLeast(0)),
        )

        is AppPreferencesIntent.SetKeepWearDisplayOn -> current.copy(
            display = current.display.copy(keepWearDisplayOn = intent.enabled),
        )

        is AppPreferencesIntent.SetKeepPhoneDisplayOn -> current.copy(
            display = current.display.copy(keepPhoneDisplayOn = intent.enabled),
        )

        is AppPreferencesIntent.SetThemeMode -> current.copy(
            display = current.display.copy(themeMode = intent.mode),
        )

        is AppPreferencesIntent.SetDynamicColors -> current.copy(
            display = current.display.copy(dynamicColors = intent.enabled),
        )

        is AppPreferencesIntent.SetStopOnLowBattery -> current.copy(
            recording = current.recording.copy(stopRecordingOnLowBattery = intent.enabled),
        )

        is AppPreferencesIntent.SetSensorSamplingPeriod -> current.copy(
            recording = current.recording.copy(sensorSamplingPeriod = intent.period),
        )

        is AppPreferencesIntent.SetWakeLock -> current.copy(
            recording = current.recording.copy(useWakeLock = intent.enabled),
        )
    }
}
