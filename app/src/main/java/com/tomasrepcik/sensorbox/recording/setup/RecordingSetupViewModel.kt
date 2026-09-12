package com.tomasrepcik.sensorbox.recording.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesIntent
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock
import com.tomasrepcik.sensorbox.recording.RecordingControlUseCase
import com.tomasrepcik.sensorbox.recording.RecordingMessage
import com.tomasrepcik.sensorbox.recording.RecordingPermissionsUseCase
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.active.RecordingSessionStateReducer
import com.tomasrepcik.sensorbox.recording.archive.RecordingArchiveRepository
import com.tomasrepcik.sensorbox.recording.archive.RecordingArchiveSelection
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class RecordingSetupViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val recordingArchive: RecordingArchiveRepository,
    private val permissions: RecordingPermissionsUseCase,
    private val recording: RecordingControlUseCase,
    private val sessionStore: RecordingSessionStore,
    private val appFailures: AppFailureStore,
    private val syncLock: MeasurementSyncLock = MeasurementSyncLock(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(RecordingState(recordingArchivePath = readRecordingArchivePath()))
    private val mutableEffects = Channel<RecordingSetupEffect>(Channel.BUFFERED)
    private var startJob: Job? = null

    val state: StateFlow<RecordingState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        observePreferences()
        observeRecordingSession()
        observeRecordingStops()
        viewModelScope.launch {
            syncLock.busy.collect { busy ->
                mutableState.value = state.value.copy(isSyncingWatch = busy)
            }
        }
    }

    fun accept(intent: RecordingSetupIntent) {
        if ((intent == RecordingSetupIntent.StartRecording || intent == RecordingSetupIntent.ChooseRecordingArchive) &&
            syncLock.busy.value
        ) {
            appFailures.show(AppError(AppErrorCode.CONFLICT, "Wait for watch sync to finish first"))
            return
        }
        intent.toPreferencesIntent()?.let {
            updatePreference(it)
            return
        }
        when (intent) {
            RecordingSetupIntent.StartRecording -> startRecording()

            RecordingSetupIntent.ChooseRecordingArchive ->
                mutableEffects.trySend(RecordingSetupEffect.PickRecordingArchive)

            else -> mutableState.value = RecordingSetupReducer.reduce(state.value, intent)
        }
    }

    fun handleRecordingArchiveResult(selection: RecordingArchiveSelection) {
        if (selection is RecordingArchiveSelection.Cancelled) return
        if (syncLock.busy.value) {
            appFailures.show(AppError(AppErrorCode.CONFLICT, "Wait for watch sync before changing the archive"))
            return
        }

        val persisted = recordingArchive.select(selection as RecordingArchiveSelection.Selected)
        persisted.errorOrNull()?.let(appFailures::show)
        mutableState.value = state.value.copy(
            recordingArchivePath = readRecordingArchivePath(),
            message = if (persisted.isSuccess) RecordingMessage.NONE else RecordingMessage.RECORDING_ARCHIVE_REQUIRED,
            errorCode = persisted.errorOrNull()?.code,
        )
    }

    fun handlePermissionResult() {
        val request = state.value.toRecordingSetup()
        if (permissions.missingPermissions(request).isEmpty()) {
            startRecording()
        } else {
            showMessage(RecordingMessage.PERMISSION_REQUIRED, AppErrorCode.PERMISSION)
        }
    }

    fun refreshRecordingArchive() {
        mutableState.value = state.value.copy(recordingArchivePath = readRecordingArchivePath())
    }

    fun reportFailure(error: AppError) {
        appFailures.show(error)
    }

    private fun RecordingSetupIntent.toPreferencesIntent(): AppPreferencesIntent? = when (this) {
        is RecordingSetupIntent.SetSamplingPeriod -> AppPreferencesIntent.SetSensorSamplingPeriod(index)
        is RecordingSetupIntent.SetStopOnLowBattery -> AppPreferencesIntent.SetStopOnLowBattery(enabled)
        is RecordingSetupIntent.SetWakeLock -> AppPreferencesIntent.SetWakeLock(enabled)
        is RecordingSetupIntent.SetKeepScreenAwake -> AppPreferencesIntent.SetKeepPhoneDisplayOn(enabled)
        is RecordingSetupIntent.SetGpsInterval -> AppPreferencesIntent.SetGpsInterval(seconds)
        is RecordingSetupIntent.SetGpsDistance -> AppPreferencesIntent.SetGpsMinDistance(meters)
        else -> null
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { result ->
                result.fold(
                    onSuccess = { preferences ->
                        mutableState.value = state.value.copy(preferences = preferences, errorCode = null)
                    },
                    onFailure = appFailures::show,
                )
            }
        }
    }

    private fun observeRecordingSession() {
        viewModelScope.launch {
            sessionStore.state.collect { session ->
                mutableState.value = RecordingSessionStateReducer.sessionChanged(state.value, session)
            }
        }
    }

    private fun observeRecordingStops() {
        viewModelScope.launch {
            sessionStore.events.collect { event ->
                val currentState = state.value
                if (currentState.isStarting && currentState.session is RecordingSessionState.Idle) {
                    mutableState.value = RecordingSetupReducer.recordingStartFailed(currentState)
                }
                event.result.errorOrNull()?.let(appFailures::show)
            }
        }
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
                mutableEffects.trySend(RecordingSetupEffect.PickRecordingArchive)
                showMessage(RecordingMessage.RECORDING_ARCHIVE_REQUIRED, AppErrorCode.STORAGE)
                return
            }
        }
        requestMissingPermissions(request)?.let {
            mutableEffects.trySend(RecordingSetupEffect.RequestPermissions(it))
            mutableState.value = state.value.copy(errorCode = AppErrorCode.PERMISSION)
            return
        }
        startRecordingAfterDelay(request, state.value.startDelaySeconds)
    }

    private fun requestMissingPermissions(request: RecordingSetup): Set<String>? =
        permissions.missingPermissions(request).takeIf(Set<String>::isNotEmpty)

    private fun readRecordingArchivePath(): String? = recordingArchive.path().fold(
        onSuccess = { it },
        onFailure = {
            appFailures.show(it)
            null
        },
    )

    private fun startRecordingAfterDelay(request: RecordingSetup, countdownSeconds: Int) {
        mutableState.value = RecordingSetupReducer.recordingStartRequested(state.value, countdownSeconds)
        startJob = viewModelScope.launch {
            waitForStartCountdown(countdownSeconds)
            val result = if (syncLock.busy.value) {
                AppResult.failure(AppError(AppErrorCode.CONFLICT, "Wait for watch sync to finish first"))
            } else {
                recording.start(request)
            }
            if (result.isFailure) {
                mutableState.value = RecordingSetupReducer.recordingStartFailed(state.value)
                showFailure(result.errorOrNull())
            }
        }
    }

    private suspend fun waitForStartCountdown(seconds: Int) {
        for (remaining in seconds downTo 1) {
            mutableState.value = RecordingSetupReducer.recordingCountdownChanged(state.value, remaining)
            delay(1_000L.milliseconds)
        }
        mutableState.value = RecordingSetupReducer.recordingCountdownChanged(state.value, null)
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
