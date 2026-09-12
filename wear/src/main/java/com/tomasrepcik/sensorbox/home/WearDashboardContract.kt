package com.tomasrepcik.sensorbox.home

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.menu.WearMenuDestination
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.sources.WatchSensorDescriptor

enum class WearRoute { MENU, RECORD, LIVE, SETTINGS, ACTIVE }

data class WearDashboardState(
    val route: WearRoute = WearRoute.MENU,
    val sensors: List<WatchSensorDescriptor> = emptyList(),
    val selectedSensorIds: Set<Int> = emptySet(),
    val includesGps: Boolean = false,
    val liveSensorType: Int? = null,
    val liveSamples: List<List<Float>> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val activeSession: RecordingSessionState.Running? = null,
    val isStopping: Boolean = false,
    val isSyncing: Boolean = false,
    val isWaitingForPermissions: Boolean = false,
    val message: WearDashboardMessage? = null,
    val visibleFailureCode: AppErrorCode? = null,
)

sealed interface WearDashboardMessage {
    data object PickSource : WearDashboardMessage
    data object PermissionRequired : WearDashboardMessage
    data object SensorUnavailable : WearDashboardMessage
}

sealed interface WearDashboardIntent {
    data class Open(val destination: WearMenuDestination) : WearDashboardIntent
    data object Back : WearDashboardIntent
    data class ToggleSensor(val sensorType: Int) : WearDashboardIntent
    data object ToggleGps : WearDashboardIntent
    data object StartRecording : WearDashboardIntent
    data class PermissionsResolved(val granted: Boolean) : WearDashboardIntent
    data object StopRecording : WearDashboardIntent
    data class ObserveSensor(val sensorType: Int) : WearDashboardIntent
    data class SetSamplingPeriod(val index: Int) : WearDashboardIntent
    data object ToggleBatteryRestriction : WearDashboardIntent
    data object ToggleWakeLock : WearDashboardIntent
    data object ToggleDisplay : WearDashboardIntent
    data object DismissFailure : WearDashboardIntent
}

sealed interface WearDashboardEffect {
    data class RequestPermissions(val permissions: Set<String>) : WearDashboardEffect
    data object OpenPhone : WearDashboardEffect
    data class OpenUrl(val destination: WearMenuDestination) : WearDashboardEffect
}

object WearDashboardReducer {
    fun reduce(state: WearDashboardState, intent: WearDashboardIntent): WearDashboardState = when (intent) {
        is WearDashboardIntent.Open -> state.copy(
            route = intent.destination.toRoute(),
            liveSensorType = null,
            liveSamples = emptyList(),
            message = null,
        )

        WearDashboardIntent.Back -> state.copy(
            route = if (state.activeSession == null) WearRoute.MENU else WearRoute.ACTIVE,
            message = null,
        )

        is WearDashboardIntent.ToggleSensor -> state.toggleSensor(intent.sensorType)

        WearDashboardIntent.ToggleGps -> state.copy(includesGps = !state.includesGps)

        is WearDashboardIntent.ObserveSensor -> state.selectLiveSensor(intent.sensorType)

        else -> state
    }

    fun recordingSessionChanged(state: WearDashboardState, session: RecordingSessionState) = when (session) {
        is RecordingSessionState.Running -> state.copy(
            route = WearRoute.ACTIVE,
            activeSession = session,
            isStopping = false,
        )

        is RecordingSessionState.Stopping -> state.copy(isStopping = true)
        is RecordingSessionState.Idle if state.route == WearRoute.ACTIVE ->
            state.copy(route = WearRoute.MENU, activeSession = null, isStopping = false)

        else -> state
    }

    private fun WearDashboardState.selectLiveSensor(sensorType: Int) = copy(
        route = WearRoute.LIVE,
        liveSensorType = sensorType,
        liveSamples = emptyList(),
    )

    private fun WearDashboardState.toggleSensor(sensorType: Int): WearDashboardState {
        val nextIds = if (sensorType in selectedSensorIds) {
            selectedSensorIds - sensorType
        } else {
            selectedSensorIds + sensorType
        }
        return copy(selectedSensorIds = nextIds, message = null)
    }

    private fun WearMenuDestination.toRoute(): WearRoute = when (this) {
        WearMenuDestination.RECORD -> WearRoute.RECORD
        WearMenuDestination.LIVE_SENSOR -> WearRoute.LIVE
        WearMenuDestination.SETTINGS -> WearRoute.SETTINGS
        else -> WearRoute.MENU
    }
}
