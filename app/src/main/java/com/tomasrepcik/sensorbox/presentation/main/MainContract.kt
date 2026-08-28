package com.tomasrepcik.sensorbox.presentation.main

import androidx.navigation3.runtime.NavKey
import com.tomasrepcik.sensorbox.sensorservices.session.RecordingSessionState
import kotlinx.serialization.Serializable

@Serializable
enum class MainRoute : NavKey {
    ONBOARDING,
    RECORD,
    ACTIVE_RECORDING,
    SENSOR_DETAILS,
    SENSOR_PREVIEW,
    RECORDING_SETUP,
    MEASUREMENTS,
    MEASUREMENT_DETAILS,
    MEASUREMENT_FILE,
    SETTINGS,
    DIAGNOSTICS,
    LICENSES,
    PRIVACY,
}

data class MainState(
    val route: MainRoute = MainRoute.ONBOARDING,
    val session: RecordingSessionState = RecordingSessionState.Idle,
    val keepScreenAwake: Boolean = false,
    val hasLoadedPreferences: Boolean = false,
)
