package com.motionapps.sensorbox.core.preferences

sealed interface AppPreferencesIntent {
    data object CompleteIntro : AppPreferencesIntent

    data object AcceptPolicy : AppPreferencesIntent

    data class SetGpsInterval(val seconds: Int) : AppPreferencesIntent

    data class SetGpsMinDistance(val meters: Int) : AppPreferencesIntent

    data class SetSensorSamplingPeriod(val period: Int) : AppPreferencesIntent

    data class SetLowBatteryRestriction(val enabled: Boolean) : AppPreferencesIntent

    data class SetWakeLock(val enabled: Boolean) : AppPreferencesIntent

    data class SetKeepPhoneDisplayOn(val enabled: Boolean) : AppPreferencesIntent

    data class SetKeepWearDisplayOn(val enabled: Boolean) : AppPreferencesIntent

    data class SetThemeMode(val mode: AppThemeMode) : AppPreferencesIntent

    data class SetDynamicColors(val enabled: Boolean) : AppPreferencesIntent
}
