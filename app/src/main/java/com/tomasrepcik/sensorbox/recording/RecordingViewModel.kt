package com.tomasrepcik.sensorbox.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesIntent
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.navigation.MainRoute
import com.tomasrepcik.sensorbox.recording.RecordingControlUseCase
import com.tomasrepcik.sensorbox.recording.RecordingPermissionsUseCase
import com.tomasrepcik.sensorbox.recording.active.ElapsedRealtimeClock
import com.tomasrepcik.sensorbox.recording.archive.RecordingArchiveRepository
import com.tomasrepcik.sensorbox.recording.archive.RecordingArchiveSelection
import com.tomasrepcik.sensorbox.recording.preview.DevicePreviewRepository
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetup
import com.tomasrepcik.sensorbox.recording.sources.AvailableRecordingSourcesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class RecordingViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val availableSources: AvailableRecordingSourcesUseCase,
    private val recordingArchive: RecordingArchiveRepository,
    private val permissions: RecordingPermissionsUseCase,
    private val recording: RecordingControlUseCase,
    private val sessionStore: RecordingSessionStore,
    private val elapsedRealtimeClock: ElapsedRealtimeClock,
    private val appFailures: AppFailureStore,
    private val devicePreview: DevicePreviewRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState())
    private val mutableEffects = Channel<RecordingEffect>(Channel.BUFFERED)
    private var elapsedJob: Job? = null
    private var startJob: Job? = null
    private var previewJob: Job? = null

    val state: StateFlow<RecordingState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        observePreferences()
        observeRecordingSession()
        observeRecordingFailures()
        observeAvailableSources()
    }

    fun accept(intent: RecordingIntent) {
        intent.toPreferencesIntent()?.let {
            updatePreference(it)
            return
        }
        when (intent) {
            RecordingIntent.StartRecording -> startRecording()

            RecordingIntent.StopRecording -> stopRecording()

            is RecordingIntent.AddAnnotation -> recording.annotate(intent.text).showFailure()

            RecordingIntent.StartGpsPreview -> startGpsPreview()

            is RecordingIntent.StartSensorPreview -> startSensorPreview(intent.sensorType)

            RecordingIntent.StopPreview -> stopPreview()

            RecordingIntent.RequestLocationPreviewPermission ->
                mutableEffects.trySend(RecordingEffect.RequestLocationPreviewPermission)

            else -> reduce(intent)
        }
    }

    private fun RecordingIntent.toPreferencesIntent(): AppPreferencesIntent? = when (this) {
        is RecordingIntent.SetSamplingPeriod -> AppPreferencesIntent.SetSensorSamplingPeriod(index)
        is RecordingIntent.SetStopOnLowBattery -> AppPreferencesIntent.SetStopOnLowBattery(enabled)
        is RecordingIntent.SetWakeLock -> AppPreferencesIntent.SetWakeLock(enabled)
        is RecordingIntent.SetKeepScreenAwake -> AppPreferencesIntent.SetKeepPhoneDisplayOn(enabled)
        is RecordingIntent.SetGpsInterval -> AppPreferencesIntent.SetGpsInterval(seconds)
        is RecordingIntent.SetGpsDistance -> AppPreferencesIntent.SetGpsMinDistance(meters)
        else -> null
    }

    fun handleRecordingArchiveResult(selection: RecordingArchiveSelection) {
        if (selection is RecordingArchiveSelection.Cancelled) return

        val persisted = recordingArchive.select(selection as RecordingArchiveSelection.Selected)
        persisted.errorOrNull()?.let(appFailures::show)
        mutableState.value = state.value.copy(
            recordingArchivePath = readRecordingArchivePath(),
            message = if (persisted.isSuccess) RecordingMessage.NONE else RecordingMessage.RECORDING_ARCHIVE_REQUIRED,
            errorCode = persisted.errorOrNull()?.code,
        )
    }

    fun reportFailure(error: AppError) {
        appFailures.show(error)
    }

    fun handlePermissionResult() {
        val request = state.value.toRecordingSetup()
        val missing = permissions.missingPermissions(request)
        if (missing.isEmpty()) {
            startRecording()
        } else {
            showMessage(RecordingMessage.PERMISSION_REQUIRED, AppErrorCode.PERMISSION)
        }
    }

    fun handleLocationPreviewPermissionResult() {
        startGpsPreview()
    }

    private fun startGpsPreview() {
        previewJob?.cancel()
        val preferences = state.value.preferences.recording
        previewJob = viewModelScope.launch {
            devicePreview.observeGps(
                preferences.gpsIntervalSeconds,
                preferences.gpsMinDistanceMeters,
            ).collect { result ->
                when (result) {
                    is AppResult.Success -> mutableState.value = state.value.copy(gpsPreview = result.value)
                    is AppResult.Failure -> appFailures.show(result.error)
                }
            }
        }
    }

    private fun startSensorPreview(sensorType: Int) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            devicePreview.observeSensor(sensorType).collect { result ->
                when (result) {
                    is AppResult.Success -> mutableState.value = state.value.copy(sensorPreview = result.value)
                    is AppResult.Failure -> appFailures.show(result.error)
                }
            }
        }
    }

    private fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
    }

    private fun initialState() = RecordingState(
        sensors = availableSources.current.phoneSensors,
        watchSensors = availableSources.current.watchSensors,
        isWatchConnected = availableSources.current.isWatchConnected,
        recordingArchivePath = readRecordingArchivePath(),
    )

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { result ->
                result.fold(
                    onSuccess = { preferences ->
                        mutableState.value = state.value.copy(
                            preferences = preferences,
                            errorCode = null,
                        )
                    },
                    onFailure = { error ->
                        appFailures.show(error)
                    },
                )
            }
        }
    }

    private fun observeRecordingSession() {
        viewModelScope.launch {
            sessionStore.state.collect { session ->
                onSessionChanged(session)
            }
        }
    }

    private fun observeRecordingFailures() {
        viewModelScope.launch {
            sessionStore.events.collect { event ->
                event.result.errorOrNull()?.let(appFailures::show)
            }
        }
    }

    private fun onSessionChanged(session: RecordingSessionState) {
        elapsedJob?.cancel()
        mutableState.value = RecordingReducer.recordingSessionChanged(state.value, session)
        if (session is RecordingSessionState.Running) startElapsedTicker(session.startedAtElapsedRealtime)
    }

    private fun startElapsedTicker(startedAt: Long) {
        elapsedJob = viewModelScope.launch {
            while (isActive) {
                val seconds = (elapsedRealtimeClock.nowMillis() - startedAt).coerceAtLeast(0) / 1_000L
                mutableState.value = state.value.copy(elapsedSeconds = seconds)
                delay(1_000L)
            }
        }
    }

    private fun observeAvailableSources() {
        viewModelScope.launch {
            availableSources.observe().catch { cause ->
                appFailures.show(
                    AppError(
                        AppErrorCode.UNKNOWN,
                        "Observe available recording sources",
                        "Recording source observation failed",
                        cause,
                    ),
                )
            }.collect { sources ->
                mutableState.value = state.value.copy(
                    sensors = sources.phoneSensors,
                    isWatchConnected = sources.isWatchConnected,
                    watchSensors = sources.watchSensors,
                )
            }
        }
    }

    private fun reduce(intent: RecordingIntent) {
        val next = RecordingReducer.reduce(state.value, intent)
        mutableState.value = next.state
        next.effect?.let(mutableEffects::trySend)
    }

    @Suppress("ReturnCount")
    private fun startRecording() {
        if (state.value.isStarting) return
        val request = state.value.toRecordingSetup()
        if (!request.hasAnySource()) {
            showMessage(RecordingMessage.PICK_AT_LEAST_ONE_SOURCE, AppErrorCode.VALIDATION)
            return
        }
        when (val selected = recordingArchive.isSelected()) {
            is AppResult.Failure -> {
                appFailures.show(selected.error)
                return
            }

            is AppResult.Success -> if (!selected.value) {
                reduce(RecordingIntent.ChooseRecordingArchive)
                showMessage(RecordingMessage.RECORDING_ARCHIVE_REQUIRED, AppErrorCode.STORAGE)
                return
            }
        }
        requestMissingPermissions(request)?.let {
            mutableEffects.trySend(RecordingEffect.RequestPermissions(it))
            mutableState.value = state.value.copy(errorCode = AppErrorCode.PERMISSION)
            return
        }
        startRecordingAfterDelay(request, state.value.startDelaySeconds)
    }

    private fun requestMissingPermissions(request: RecordingSetup): Set<String>? {
        val missingPermissions = permissions.missingPermissions(request)
        return missingPermissions.takeIf(Set<String>::isNotEmpty)
    }

    private fun readRecordingArchivePath(): String? = recordingArchive.path().fold(
        onSuccess = { it },
        onFailure = {
            appFailures.show(it)
            null
        },
    )

    private fun startRecordingAfterDelay(request: RecordingSetup, countdownSeconds: Int) {
        mutableState.value = RecordingReducer.recordingStartRequested(state.value, countdownSeconds)
        startJob = viewModelScope.launch {
            waitForStartCountdown(countdownSeconds)
            val result = recording.start(request)
            if (result.isFailure) {
                mutableState.value = RecordingReducer.recordingStartFailed(state.value)
                showFailure(result.errorOrNull())
            }
        }
    }

    private suspend fun waitForStartCountdown(seconds: Int) {
        for (remaining in seconds downTo 1) {
            mutableState.value = RecordingReducer.recordingCountdownChanged(state.value, remaining)
            delay(1_000L)
        }
        mutableState.value = RecordingReducer.recordingCountdownChanged(state.value, null)
    }

    private fun stopRecording() {
        if (state.value.isStarting && state.value.session is RecordingSessionState.Idle) {
            startJob?.cancel()
            startJob = null
            mutableState.value = RecordingReducer.recordingStartFailed(state.value)
            return
        }
        viewModelScope.launch {
            val result = recording.stop()
            if (result.isSuccess) {
                mutableEffects.send(RecordingEffect.Navigate(MainRoute.RECORD))
            } else {
                result.showFailure()
            }
        }
    }

    private fun updatePreference(intent: AppPreferencesIntent) {
        viewModelScope.launch { preferencesRepository.dispatch(intent).showFailure() }
    }

    private fun showMessage(message: RecordingMessage, errorCode: AppErrorCode? = null) {
        mutableState.value = state.value.copy(message = message, errorCode = errorCode)
    }

    private fun AppResult<*>.showFailure() {
        if (isFailure) showFailure(errorOrNull())
    }

    private fun showFailure(error: AppError?) {
        when {
            error == null -> appFailures.show(AppError(AppErrorCode.UNKNOWN, "Run recording operation"))

            error.code == AppErrorCode.PERMISSION && error.context["source"] == "watch" ->
                showMessage(RecordingMessage.WATCH_PERMISSION_REQUIRED, AppErrorCode.PERMISSION)

            error.code == AppErrorCode.PERMISSION ->
                showMessage(RecordingMessage.PERMISSION_REQUIRED, AppErrorCode.PERMISSION)

            else -> appFailures.show(error)
        }
    }

    private fun RecordingSetup.hasAnySource(): Boolean = sensorIds.isNotEmpty() || includesGps ||
        watchSensorIds.isNotEmpty() || watchIncludesGps || activityRecognition || significantMotion
}
