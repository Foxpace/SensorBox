package com.motionapps.sensorbox.presentation.main

import androidx.navigation3.runtime.NavKey
import com.motionapps.sensorbox.core.preferences.AppPreferences
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
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
    LICENSES,
    PRIVACY,
}

enum class MainMessage {
    NONE,
    PICK_AT_LEAST_ONE_SOURCE,
    STORAGE_REQUIRED,
    PERMISSION_REQUIRED,
    MEASUREMENT_FAILED,
}

data class MainState(
    val route: MainRoute = MainRoute.ONBOARDING,
    val onboardingPage: Int = 0,
    val sensors: List<SensorDescriptor> = emptyList(),
    val wearSensors: List<SensorDescriptor> = emptyList(),
    val detailsSensorType: Int? = null,
    val selectedSensorIds: Set<Int> = emptySet(),
    val includesGps: Boolean = false,
    val selectedWearSensorIds: Set<Int> = emptySet(),
    val wearIncludesGps: Boolean = false,
    val customMeasurementName: String = "",
    val measurementType: String = "ENDLESS",
    val startDelaySeconds: Int = 0,
    val durationSeconds: Int = 0,
    val notes: String = "",
    val alarmOffsets: String = "",
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
    val storagePath: String? = null,
    val preferences: AppPreferences = AppPreferences(),
    val hasLoadedPreferences: Boolean = false,
    val session: MeasurementSessionState = MeasurementSessionState.Idle,
    val elapsedSeconds: Long = 0,
    val isWearConnected: Boolean = false,
    val message: MainMessage = MainMessage.NONE,
)

sealed interface MainIntent {
    data class Navigate(val route: MainRoute) : MainIntent
    data class ToggleSensor(val sensorId: Int) : MainIntent
    data class ToggleWearSensor(val sensorId: Int) : MainIntent
    data class OpenSensorDetails(val sensorType: Int?) : MainIntent
    data object ToggleGps : MainIntent
    data object ToggleWearGps : MainIntent
    data object AdvanceOnboarding : MainIntent
    data object RetreatOnboarding : MainIntent
    data object CompleteOnboarding : MainIntent
    data object OpenPrivacyPolicy : MainIntent
    data object OpenTermsOfUse : MainIntent
    data object RequestBatteryOptimizationExemption : MainIntent
    data object ShareDiagnosticsText : MainIntent
    data object ShareDiagnosticsFile : MainIntent
    data object ChooseStorage : MainIntent
    data object OpenMeasurementSetup : MainIntent
    data object ReturnToSensorSelection : MainIntent
    data object StartMeasurement : MainIntent
    data object StopMeasurement : MainIntent
    data object ClearMessage : MainIntent
    data class SetCustomMeasurementName(val value: String) : MainIntent
    data class SetMeasurementType(val value: String) : MainIntent
    data class SetStartDelay(val seconds: Int) : MainIntent
    data class SetDuration(val seconds: Int) : MainIntent
    data class SetNotes(val value: String) : MainIntent
    data class SetAlarmOffsets(val value: String) : MainIntent
    data class SetActivityRecognition(val enabled: Boolean) : MainIntent
    data class SetActivityRecognitionPeriod(val seconds: Int) : MainIntent
    data class SetSignificantMotion(val enabled: Boolean) : MainIntent
    data class AddAnnotation(val text: String) : MainIntent
    data class SetSamplingPeriod(val index: Int) : MainIntent
    data class SetLowBatteryRestriction(val enabled: Boolean) : MainIntent
    data class SetWakeLock(val enabled: Boolean) : MainIntent
    data class SetKeepScreenAwake(val enabled: Boolean) : MainIntent
    data class SetGpsInterval(val seconds: Int) : MainIntent
    data class SetGpsDistance(val meters: Int) : MainIntent
}

sealed interface MainEffect {
    data object PickStorageDirectory : MainEffect
    data object OpenPrivacyPolicy : MainEffect
    data object OpenTermsOfUse : MainEffect
    data object RequestBatteryOptimizationExemption : MainEffect
    data object ShareDiagnosticsText : MainEffect
    data object ShareDiagnosticsFile : MainEffect
    data class RequestPermissions(val permissions: Set<String>) : MainEffect
}

data class MainNext(val state: MainState, val effect: MainEffect? = null)

object MainReducer {
    fun reduce(state: MainState, intent: MainIntent): MainNext = when (intent) {
        MainIntent.AdvanceOnboarding -> MainNext(state.copy(onboardingPage = state.onboardingPage + 1))
        MainIntent.RetreatOnboarding -> MainNext(state.retreatOnboarding())
        is MainIntent.OpenSensorDetails -> MainNext(state.openSensorDetails(intent.sensorType))
        else -> reduceGeneralIntent(state, intent)
    }

    private fun reduceGeneralIntent(state: MainState, intent: MainIntent): MainNext = when (intent) {
        MainIntent.ChooseStorage -> MainNext(state, MainEffect.PickStorageDirectory)
        MainIntent.ClearMessage -> MainNext(state.copy(message = MainMessage.NONE))
        MainIntent.OpenPrivacyPolicy -> MainNext(state, MainEffect.OpenPrivacyPolicy)
        MainIntent.OpenTermsOfUse -> MainNext(state, MainEffect.OpenTermsOfUse)
        MainIntent.RequestBatteryOptimizationExemption -> batteryOptimizationEffect(state)
        MainIntent.ShareDiagnosticsText -> MainNext(state, MainEffect.ShareDiagnosticsText)
        MainIntent.ShareDiagnosticsFile -> MainNext(state, MainEffect.ShareDiagnosticsFile)
        MainIntent.OpenMeasurementSetup -> MainNext(state.openMeasurementSetup())
        MainIntent.ReturnToSensorSelection -> MainNext(state.returnToSensorSelection())
        else -> reduceSelectionIntent(state, intent)
    }

    private fun reduceSelectionIntent(state: MainState, intent: MainIntent): MainNext = when (intent) {
        MainIntent.ToggleGps -> MainNext(state.copy(includesGps = !state.includesGps))
        MainIntent.ToggleWearGps -> MainNext(state.copy(wearIncludesGps = !state.wearIncludesGps))
        is MainIntent.Navigate -> MainNext(state.copy(route = intent.route))
        is MainIntent.ToggleSensor -> MainNext(state.toggleSensor(intent.sensorId))
        is MainIntent.ToggleWearSensor -> MainNext(state.toggleWearSensor(intent.sensorId))
        else -> reduceConfigurationIntent(state, intent)
    }

    private fun reduceConfigurationIntent(state: MainState, intent: MainIntent): MainNext = when (intent) {
        is MainIntent.SetCustomMeasurementName -> MainNext(state.copy(customMeasurementName = intent.value))
        is MainIntent.SetMeasurementType -> MainNext(state.copy(measurementType = intent.value))
        is MainIntent.SetStartDelay -> MainNext(state.copy(startDelaySeconds = intent.seconds.coerceAtLeast(0)))
        is MainIntent.SetDuration -> MainNext(state.copy(durationSeconds = intent.seconds.coerceAtLeast(0)))
        is MainIntent.SetNotes -> MainNext(state.copy(notes = intent.value))
        is MainIntent.SetAlarmOffsets -> MainNext(state.copy(alarmOffsets = intent.value))
        is MainIntent.SetActivityRecognition -> MainNext(state.copy(activityRecognition = intent.enabled))
        is MainIntent.SetActivityRecognitionPeriod -> MainNext(state.withActivityPeriod(intent.seconds))
        is MainIntent.SetSignificantMotion -> MainNext(state.copy(significantMotion = intent.enabled))
        else -> MainNext(state)
    }

    private fun batteryOptimizationEffect(state: MainState) =
        MainNext(state, MainEffect.RequestBatteryOptimizationExemption)

    private fun MainState.retreatOnboarding() = copy(onboardingPage = (onboardingPage - 1).coerceAtLeast(0))

    private fun MainState.openSensorDetails(sensorType: Int?) =
        copy(route = MainRoute.SENSOR_DETAILS, detailsSensorType = sensorType)

    private fun MainState.openMeasurementSetup() = copy(route = MainRoute.SETUP, message = MainMessage.NONE)

    private fun MainState.returnToSensorSelection() = copy(route = MainRoute.RECORD, message = MainMessage.NONE)

    private fun MainState.toggleSensor(sensorId: Int): MainState {
        val updated = selectedSensorIds.toMutableSet()
        if (!updated.add(sensorId)) updated.remove(sensorId)
        return copy(selectedSensorIds = updated, message = MainMessage.NONE)
    }

    private fun MainState.toggleWearSensor(sensorId: Int): MainState {
        val updated = selectedWearSensorIds.toMutableSet()
        if (!updated.add(sensorId)) updated.remove(sensorId)
        return copy(selectedWearSensorIds = updated, message = MainMessage.NONE)
    }
}

private fun MainState.withActivityPeriod(seconds: Int) =
    copy(activityRecognitionPeriodSeconds = seconds.coerceAtLeast(1))
