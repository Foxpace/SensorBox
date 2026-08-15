package com.motionapps.sensorbox.core.preferences

object AppPreferencesReducer {
    fun reduce(current: AppPreferences, intent: AppPreferencesIntent): AppPreferences = when (intent) {
        AppPreferencesIntent.AcceptPolicy -> current.copy(hasAcceptedPolicy = true)

        AppPreferencesIntent.CompleteIntro -> current.copy(hasCompletedIntro = true)

        is AppPreferencesIntent.SetGpsInterval -> current.copy(
            gpsIntervalSeconds = intent.seconds.coerceAtLeast(1),
        )

        is AppPreferencesIntent.SetGpsMinDistance -> current.copy(
            gpsMinDistanceMeters = intent.meters.coerceAtLeast(0),
        )

        is AppPreferencesIntent.SetKeepWearDisplayOn -> current.copy(
            keepWearDisplayOn = intent.enabled,
        )

        is AppPreferencesIntent.SetKeepPhoneDisplayOn -> current.copy(
            keepPhoneDisplayOn = intent.enabled,
        )

        is AppPreferencesIntent.SetLowBatteryRestriction -> current.copy(
            restrictMeasurementOnLowBattery = intent.enabled,
        )

        is AppPreferencesIntent.SetSensorSamplingPeriod -> current.copy(
            sensorSamplingPeriod = intent.period,
        )

        is AppPreferencesIntent.SetWakeLock -> current.copy(useWakeLock = intent.enabled)
    }
}
