package com.motionapps.sensorbox.presentation.main

import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.preferences.AppPreferences
import com.motionapps.sensorbox.domain.measurement.MeasurementRequest
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
import com.motionapps.sensorservices.session.MeasurementSessionState

enum class RecordingMessage {
    NONE,
    PICK_AT_LEAST_ONE_SOURCE,
    STORAGE_REQUIRED,
    PERMISSION_REQUIRED,
    MEASUREMENT_FAILED,
}

data class RecordingState(
    val sensors: List<SensorDescriptor> = emptyList(),
    val wearSensors: List<SensorDescriptor> = emptyList(),
    val detailsSensorType: Int? = null,
    val selectedSensorIds: Set<Int> = emptySet(),
    val includesGps: Boolean = false,
    val selectedWearSensorIds: Set<Int> = emptySet(),
    val wearIncludesGps: Boolean = false,
    val customMeasurementName: String = "",
    val startDelaySeconds: Int = 0,
    val durationSeconds: Int = 0,
    val notes: String = "",
    val alarmOffsets: String = "",
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
    val storagePath: String? = null,
    val preferences: AppPreferences = AppPreferences(),
    val session: MeasurementSessionState = MeasurementSessionState.Idle,
    val elapsedSeconds: Long = 0,
    val isWearConnected: Boolean = false,
    val message: RecordingMessage = RecordingMessage.NONE,
    val errorCode: AppErrorCode? = null,
) {
    fun toMeasurementRequest() = MeasurementRequest(
        sensorIds = selectedSensorIds,
        includesGps = includesGps,
        samplingPeriodIndex = preferences.recording.sensorSamplingPeriod,
        stopOnLowBattery = preferences.recording.restrictMeasurementOnLowBattery,
        useWakeLock = preferences.recording.useWakeLock,
        gpsIntervalSeconds = preferences.recording.gpsIntervalSeconds,
        gpsMinDistanceMeters = preferences.recording.gpsMinDistanceMeters,
        wearSensorIds = selectedWearSensorIds,
        wearIncludesGps = wearIncludesGps,
        customName = customMeasurementName,
        delaySeconds = startDelaySeconds,
        durationSeconds = durationSeconds.coerceAtLeast(0),
        notes = notes.lines().map(String::trim).filter(String::isNotEmpty),
        alarmOffsetsSeconds = alarmOffsets.split(',', ';', ' ')
            .mapNotNull(String::toIntOrNull).filter { it >= 0 },
        activityRecognition = activityRecognition,
        activityRecognitionPeriodSeconds = activityRecognitionPeriodSeconds,
        significantMotion = significantMotion,
    )
}

sealed interface RecordingIntent {
    data class Navigate(val route: MainRoute) : RecordingIntent
    data class ToggleSensor(val sensorId: Int) : RecordingIntent
    data class ToggleWearSensor(val sensorId: Int) : RecordingIntent
    data class OpenSensorDetails(val sensorType: Int?) : RecordingIntent
    data object ToggleGps : RecordingIntent
    data object ToggleWearGps : RecordingIntent
    data object ChooseStorage : RecordingIntent
    data object OpenMeasurementSetup : RecordingIntent
    data object ReturnToSensorSelection : RecordingIntent
    data object StartMeasurement : RecordingIntent
    data object StopMeasurement : RecordingIntent
    data object ClearMessage : RecordingIntent
    data class SetCustomMeasurementName(val value: String) : RecordingIntent
    data class SetStartDelay(val seconds: Int) : RecordingIntent
    data class SetDuration(val seconds: Int) : RecordingIntent
    data class SetNotes(val value: String) : RecordingIntent
    data class SetAlarmOffsets(val value: String) : RecordingIntent
    data class SetActivityRecognition(val enabled: Boolean) : RecordingIntent
    data class SetActivityRecognitionPeriod(val seconds: Int) : RecordingIntent
    data class SetSignificantMotion(val enabled: Boolean) : RecordingIntent
    data class AddAnnotation(val text: String) : RecordingIntent
    data class SetSamplingPeriod(val index: Int) : RecordingIntent
    data class SetLowBatteryRestriction(val enabled: Boolean) : RecordingIntent
    data class SetWakeLock(val enabled: Boolean) : RecordingIntent
    data class SetKeepScreenAwake(val enabled: Boolean) : RecordingIntent
    data class SetGpsInterval(val seconds: Int) : RecordingIntent
    data class SetGpsDistance(val meters: Int) : RecordingIntent
}

sealed interface RecordingEffect {
    data object PickStorageDirectory : RecordingEffect
    data class RequestPermissions(val permissions: Set<String>) : RecordingEffect
    data class Navigate(val route: MainRoute) : RecordingEffect
}

data class RecordingNext(val state: RecordingState, val effect: RecordingEffect? = null)

object RecordingReducer {
    fun reduce(state: RecordingState, intent: RecordingIntent): RecordingNext = reduceNavigation(state, intent)
        ?: reduceSelection(state, intent)
        ?: reduceConfiguration(state, intent)

    private fun reduceNavigation(state: RecordingState, intent: RecordingIntent): RecordingNext? = when (intent) {
        RecordingIntent.ChooseStorage -> RecordingNext(state, RecordingEffect.PickStorageDirectory)

        is RecordingIntent.Navigate -> RecordingNext(state, RecordingEffect.Navigate(intent.route))

        is RecordingIntent.OpenSensorDetails -> RecordingNext(
            state.copy(detailsSensorType = intent.sensorType),
            RecordingEffect.Navigate(MainRoute.SENSOR_DETAILS),
        )

        RecordingIntent.OpenMeasurementSetup -> RecordingNext(
            state.copy(message = RecordingMessage.NONE),
            RecordingEffect.Navigate(MainRoute.SETUP),
        )

        RecordingIntent.ReturnToSensorSelection -> RecordingNext(
            state.copy(message = RecordingMessage.NONE),
            RecordingEffect.Navigate(MainRoute.RECORD),
        )

        else -> null
    }

    private fun reduceSelection(state: RecordingState, intent: RecordingIntent): RecordingNext? = when (intent) {
        RecordingIntent.ClearMessage -> RecordingNext(state.copy(message = RecordingMessage.NONE, errorCode = null))
        RecordingIntent.ToggleGps -> RecordingNext(state.copy(includesGps = !state.includesGps))
        RecordingIntent.ToggleWearGps -> RecordingNext(state.copy(wearIncludesGps = !state.wearIncludesGps))
        is RecordingIntent.ToggleSensor -> RecordingNext(state.toggleSensor(intent.sensorId))
        is RecordingIntent.ToggleWearSensor -> RecordingNext(state.toggleWearSensor(intent.sensorId))
        else -> null
    }

    private fun reduceConfiguration(state: RecordingState, intent: RecordingIntent): RecordingNext = when (intent) {
        is RecordingIntent.SetCustomMeasurementName -> RecordingNext(state.copy(customMeasurementName = intent.value))

        is RecordingIntent.SetStartDelay -> RecordingNext(
            state.copy(startDelaySeconds = intent.seconds.coerceAtLeast(0)),
        )

        is RecordingIntent.SetDuration -> RecordingNext(state.copy(durationSeconds = intent.seconds.coerceAtLeast(0)))

        is RecordingIntent.SetNotes -> RecordingNext(state.copy(notes = intent.value))

        is RecordingIntent.SetAlarmOffsets -> RecordingNext(state.copy(alarmOffsets = intent.value))

        is RecordingIntent.SetActivityRecognition -> RecordingNext(state.copy(activityRecognition = intent.enabled))

        is RecordingIntent.SetActivityRecognitionPeriod -> RecordingNext(
            state.copy(activityRecognitionPeriodSeconds = intent.seconds.coerceAtLeast(1)),
        )

        is RecordingIntent.SetSignificantMotion -> RecordingNext(state.copy(significantMotion = intent.enabled))

        else -> RecordingNext(state)
    }

    private fun RecordingState.toggleSensor(sensorId: Int): RecordingState {
        val updated = selectedSensorIds.toMutableSet()
        if (!updated.add(sensorId)) updated.remove(sensorId)
        return copy(selectedSensorIds = updated, message = RecordingMessage.NONE, errorCode = null)
    }

    private fun RecordingState.toggleWearSensor(sensorId: Int): RecordingState {
        val updated = selectedWearSensorIds.toMutableSet()
        if (!updated.add(sensorId)) updated.remove(sensorId)
        return copy(selectedWearSensorIds = updated, message = RecordingMessage.NONE, errorCode = null)
    }
}
