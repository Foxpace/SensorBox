package com.motionapps.sensorbox.core.preferences

data class AppPreferences(
    val hasCompletedIntro: Boolean = false,
    val hasAcceptedPolicy: Boolean = false,
    val gpsIntervalSeconds: Int = 10,
    val gpsMinDistanceMeters: Int = 20,
    val sensorSamplingPeriod: Int = 0,
    val restrictMeasurementOnLowBattery: Boolean = true,
    val useWakeLock: Boolean = false,
    val keepPhoneDisplayOn: Boolean = false,
    val keepWearDisplayOn: Boolean = false,
)
