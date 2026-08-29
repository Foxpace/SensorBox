package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.navigation.MainRoute
import com.tomasrepcik.sensorbox.recording.preview.GpsPreviewData
import com.tomasrepcik.sensorbox.recording.preview.SensorPreviewData
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetup
import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor

enum class RecordingMessage {
    NONE,
    PICK_AT_LEAST_ONE_SOURCE,
    RECORDING_ARCHIVE_REQUIRED,
    PERMISSION_REQUIRED,
    WATCH_PERMISSION_REQUIRED,
    RECORDING_FAILED,
}

enum class RecordingDevice {
    PHONE,
    WATCH,
}

data class RecordingState(
    val sensors: List<SensorDescriptor> = emptyList(),
    val watchSensors: List<SensorDescriptor> = emptyList(),
    val detailsSensorType: Int? = null,
    val detailsDevice: RecordingDevice = RecordingDevice.PHONE,
    val selectedSensorIds: Set<Int> = emptySet(),
    val includesGps: Boolean = false,
    val selectedWatchSensorIds: Set<Int> = emptySet(),
    val watchIncludesGps: Boolean = false,
    val customMeasurementName: String = "",
    val startDelaySeconds: Int = 0,
    val durationSeconds: Int = 0,
    val notes: String = "",
    val alarmOffsets: String = "",
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
    val recordingArchivePath: String? = null,
    val preferences: AppPreferences = AppPreferences(),
    val session: RecordingSessionState = RecordingSessionState.Idle,
    val elapsedSeconds: Long = 0,
    val isWatchConnected: Boolean = false,
    val isStarting: Boolean = false,
    val startCountdownSeconds: Int? = null,
    val message: RecordingMessage = RecordingMessage.NONE,
    val errorCode: AppErrorCode? = null,
    val gpsPreview: GpsPreviewData = GpsPreviewData(hasPermission = false),
    val sensorPreview: SensorPreviewData = SensorPreviewData(isAvailable = true),
) {
    fun toRecordingSetup() = RecordingSetup(
        sensorIds = selectedSensorIds,
        includesGps = includesGps,
        samplingPeriodIndex = preferences.recording.sensorSamplingPeriod,
        stopOnLowBattery = preferences.recording.stopRecordingOnLowBattery,
        useWakeLock = preferences.recording.useWakeLock,
        gpsIntervalSeconds = preferences.recording.gpsIntervalSeconds,
        gpsMinDistanceMeters = preferences.recording.gpsMinDistanceMeters,
        watchSensorIds = selectedWatchSensorIds,
        watchIncludesGps = watchIncludesGps,
        customName = customMeasurementName,
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
    data class ToggleWatchSensor(val sensorId: Int) : RecordingIntent
    data class OpenSensorDetails(val sensorType: Int?, val device: RecordingDevice = RecordingDevice.PHONE) :
        RecordingIntent
    data object ToggleGps : RecordingIntent
    data object ToggleWatchGps : RecordingIntent
    data object ChooseRecordingArchive : RecordingIntent
    data object OpenRecordingSetup : RecordingIntent
    data object ReturnToSensorSelection : RecordingIntent
    data object StartRecording : RecordingIntent
    data object StopRecording : RecordingIntent
    data object ClearMessage : RecordingIntent
    data class SetCustomMeasurementName(val value: String) : RecordingIntent
    data class SetStartDelay(val seconds: Int) : RecordingIntent
    data class SetDuration(val seconds: Int) : RecordingIntent
    data class SetNotes(val value: String) : RecordingIntent
    data class SetAlarmOffsets(val value: String) : RecordingIntent
    data class SetActivityRecognition(val enabled: Boolean) : RecordingIntent
    data class SetActivityRecognitionPeriod(val seconds: Int) : RecordingIntent
    data class SetSignificantMotionRecording(val enabled: Boolean) : RecordingIntent
    data class AddAnnotation(val text: String) : RecordingIntent
    data class SetSamplingPeriod(val index: Int) : RecordingIntent
    data class SetStopOnLowBattery(val enabled: Boolean) : RecordingIntent
    data class SetWakeLock(val enabled: Boolean) : RecordingIntent
    data class SetKeepScreenAwake(val enabled: Boolean) : RecordingIntent
    data class SetGpsInterval(val seconds: Int) : RecordingIntent
    data class SetGpsDistance(val meters: Int) : RecordingIntent
    data object StartGpsPreview : RecordingIntent
    data class StartSensorPreview(val sensorType: Int) : RecordingIntent
    data object StopPreview : RecordingIntent
    data object RequestLocationPreviewPermission : RecordingIntent
}

sealed interface RecordingEffect {
    data object PickRecordingArchive : RecordingEffect
    data class RequestPermissions(val permissions: Set<String>) : RecordingEffect
    data object RequestLocationPreviewPermission : RecordingEffect
    data class Navigate(val route: MainRoute) : RecordingEffect
}

data class RecordingNext(val state: RecordingState, val effect: RecordingEffect? = null)

object RecordingReducer {
    fun reduce(state: RecordingState, intent: RecordingIntent): RecordingNext = reduceNavigation(state, intent)
        ?: reduceSelection(state, intent)
        ?: reduceConfiguration(state, intent)

    fun recordingStartRequested(state: RecordingState, countdownSeconds: Int): RecordingState = state.copy(
        isStarting = true,
        startCountdownSeconds = countdownSeconds.takeIf { it > 0 },
        message = RecordingMessage.NONE,
        errorCode = null,
    )

    fun recordingCountdownChanged(state: RecordingState, seconds: Int?): RecordingState = state.copy(
        startCountdownSeconds = seconds,
    )

    fun recordingStartFailed(state: RecordingState): RecordingState = state.copy(
        isStarting = false,
        startCountdownSeconds = null,
    )

    fun recordingSessionChanged(state: RecordingState, session: RecordingSessionState): RecordingState = state.copy(
        session = session,
        elapsedSeconds = 0,
        isStarting = if (session is RecordingSessionState.Running) false else state.isStarting,
        startCountdownSeconds = if (session is RecordingSessionState.Running) null else state.startCountdownSeconds,
    )

    private fun reduceNavigation(state: RecordingState, intent: RecordingIntent): RecordingNext? = when (intent) {
        RecordingIntent.ChooseRecordingArchive -> RecordingNext(state, RecordingEffect.PickRecordingArchive)

        is RecordingIntent.Navigate -> RecordingNext(state, RecordingEffect.Navigate(intent.route))

        is RecordingIntent.OpenSensorDetails -> RecordingNext(
            state.copy(
                detailsSensorType = intent.sensorType,
                detailsDevice = intent.device,
            ),
            RecordingEffect.Navigate(MainRoute.SENSOR_DETAILS),
        )

        RecordingIntent.OpenRecordingSetup -> RecordingNext(
            state.copy(message = RecordingMessage.NONE),
            RecordingEffect.Navigate(MainRoute.RECORDING_SETUP),
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
        RecordingIntent.ToggleWatchGps -> RecordingNext(state.copy(watchIncludesGps = !state.watchIncludesGps))
        is RecordingIntent.ToggleSensor -> RecordingNext(state.toggleSensor(intent.sensorId))
        is RecordingIntent.ToggleWatchSensor -> RecordingNext(state.toggleWatchSensor(intent.sensorId))
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

        is RecordingIntent.SetSignificantMotionRecording -> RecordingNext(
            state.copy(significantMotion = intent.enabled),
        )

        else -> RecordingNext(state)
    }

    private fun RecordingState.toggleSensor(sensorId: Int): RecordingState {
        val updated = selectedSensorIds.toMutableSet()
        if (!updated.add(sensorId)) updated.remove(sensorId)
        return copy(selectedSensorIds = updated, message = RecordingMessage.NONE, errorCode = null)
    }

    private fun RecordingState.toggleWatchSensor(sensorId: Int): RecordingState {
        val updated = selectedWatchSensorIds.toMutableSet()
        if (!updated.add(sensorId)) updated.remove(sensorId)
        return copy(selectedWatchSensorIds = updated, message = RecordingMessage.NONE, errorCode = null)
    }
}
