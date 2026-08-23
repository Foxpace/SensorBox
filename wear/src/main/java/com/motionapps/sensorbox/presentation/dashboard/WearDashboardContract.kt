package com.motionapps.sensorbox.presentation.dashboard

import com.motionapps.sensorbox.core.preferences.AppPreferences
import com.motionapps.sensorbox.domain.sensors.WearSensorDescriptor
import com.motionapps.sensorbox.presentation.menu.WearMenuDestination

enum class WearRoute { MENU, RECORD, LIVE, SETTINGS, ACTIVE }

data class WearDashboardState(
    val route: WearRoute = WearRoute.MENU,
    val sensors: List<WearSensorDescriptor> = emptyList(),
    val selectedSensorIds: Set<Int> = emptySet(),
    val includesGps: Boolean = false,
    val liveSensorType: Int? = null,
    val latestValue: Float? = null,
    val preferences: AppPreferences = AppPreferences(),
    val isSyncing: Boolean = false,
    val message: WearDashboardMessage? = null,
)

sealed interface WearDashboardMessage {
    data object PickSource : WearDashboardMessage
    data object PermissionRequired : WearDashboardMessage
    data object SensorUnavailable : WearDashboardMessage
    data object Syncing : WearDashboardMessage
    data object SyncFailed : WearDashboardMessage
    data class FilesSent(val count: Int) : WearDashboardMessage
}

sealed interface WearDashboardIntent {
    data class Open(val destination: WearMenuDestination) : WearDashboardIntent
    data object Back : WearDashboardIntent
    data class ToggleSensor(val sensorType: Int) : WearDashboardIntent
    data object ToggleGps : WearDashboardIntent
    data object StartMeasurement : WearDashboardIntent
    data class PermissionsResolved(val granted: Boolean) : WearDashboardIntent
    data object StopMeasurement : WearDashboardIntent
    data class ObserveSensor(val sensorType: Int) : WearDashboardIntent
    data class SetSamplingPeriod(val index: Int) : WearDashboardIntent
    data object ToggleBatteryRestriction : WearDashboardIntent
    data object ToggleWakeLock : WearDashboardIntent
    data object ToggleDisplay : WearDashboardIntent
    data object SyncMeasurements : WearDashboardIntent
}

sealed interface WearDashboardEffect {
    data class RequestPermissions(val permissions: Set<String>) : WearDashboardEffect
    data object OpenPhone : WearDashboardEffect
    data class OpenUrl(val destination: WearMenuDestination) : WearDashboardEffect
}

object WearDashboardReducer {
    fun reduce(state: WearDashboardState, intent: WearDashboardIntent): WearDashboardState = when (intent) {
        is WearDashboardIntent.Open -> state.copy(route = intent.destination.toRoute(), message = null)
        WearDashboardIntent.Back -> state.copy(route = WearRoute.MENU, message = null)
        is WearDashboardIntent.ToggleSensor -> state.toggleSensor(intent.sensorType)
        WearDashboardIntent.ToggleGps -> state.copy(includesGps = !state.includesGps)
        is WearDashboardIntent.ObserveSensor -> state.selectLiveSensor(intent.sensorType)
        else -> state
    }

    private fun WearDashboardState.selectLiveSensor(sensorType: Int) = copy(
        route = WearRoute.LIVE,
        liveSensorType = sensorType,
        latestValue = null,
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
