package com.tomasrepcik.sensorbox.navigation

import androidx.navigation3.runtime.NavKey
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.preferences.AppThemeMode
import com.tomasrepcik.sensorbox.recording.RecordingDevice
import com.tomasrepcik.sensorbox.recording.RecordingDraft
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import kotlinx.serialization.Serializable

@Serializable
enum class MainRoute : NavKey {
    ONBOARDING,
    RECORD,
    ACTIVE_RECORDING,
    MEASUREMENTS,
    SETTINGS,
    DIAGNOSTICS,
    LICENSES,
    PRIVACY,
}

@Serializable
data class MeasurementDetailsRoute(val measurementId: String) : NavKey

@Serializable
data class MeasurementLoadingRoute(val measurementId: String, val fileId: String) : NavKey

@Serializable
data class MeasurementPreviewRoute(val measurementId: String, val fileId: String) : NavKey

@Serializable
data class SensorDetailsRoute(val sensorType: Int?, val device: RecordingDevice) : NavKey

@Serializable
data class SensorPreviewRoute(val sensorType: Int?, val device: RecordingDevice) : NavKey

@Serializable
data class RecordingSetupRoute(val draft: RecordingDraft) : NavKey

data class MainState(
    val route: NavKey = MainRoute.ONBOARDING,
    val session: RecordingSessionState = RecordingSessionState.Idle,
    val keepScreenAwake: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.AUTOMATIC,
    val dynamicColors: Boolean = true,
    val hasLoadedPreferences: Boolean = false,
    val visibleFailureCode: AppErrorCode? = null,
    val replaceCurrentRoute: Boolean = false,
)
