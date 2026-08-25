package com.tomasrepcik.sensorbox.presentation.main

import androidx.navigation3.runtime.NavKey
import com.motionapps.sensorservices.session.MeasurementSessionState
import kotlinx.serialization.Serializable

@Serializable
enum class MainRoute : NavKey {
    ONBOARDING,
    RECORD,
    ACTIVE_MEASUREMENT,
    SENSOR_DETAILS,
    SENSOR_PREVIEW,
    SETUP,
    SETTINGS,
    DIAGNOSTICS,
    LICENSES,
    PRIVACY,
}

data class MainState(
    val route: MainRoute = MainRoute.ONBOARDING,
    val session: MeasurementSessionState = MeasurementSessionState.Idle,
    val keepScreenAwake: Boolean = false,
    val hasLoadedPreferences: Boolean = false,
)
