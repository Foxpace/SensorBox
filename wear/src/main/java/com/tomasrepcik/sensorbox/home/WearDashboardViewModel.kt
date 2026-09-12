package com.tomasrepcik.sensorbox.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesIntent
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock
import com.tomasrepcik.sensorbox.menu.WearMenuDestination
import com.tomasrepcik.sensorbox.recording.DefaultWatchRecordingControlUseCase
import com.tomasrepcik.sensorbox.recording.WatchRecordingPermissionUseCase
import com.tomasrepcik.sensorbox.recording.live.ObserveSensorValuesUseCase
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.recording.sources.GetWatchSensorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WearDashboardViewModel @Inject constructor(
    getSensors: GetWatchSensorsUseCase,
    private val observeSensorValues: ObserveSensorValuesUseCase,
    private val permissionUseCase: WatchRecordingPermissionUseCase,
    private val recordingControl: DefaultWatchRecordingControlUseCase,
    private val preferencesRepository: AppPreferencesRepository,
    private val sessionStore: RecordingSessionStore,
    private val appFailures: AppFailureStore,
    private val syncLock: MeasurementSyncLock = MeasurementSyncLock(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(WearDashboardState(sensors = getSensors()))
    private val mutableEffects = Channel<WearDashboardEffect>(Channel.BUFFERED)
    private var sensorJob: Job? = null

    val state = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        observePreferences()
        observeSession()
        observeRecordingFailures()
        observeFailures()
        viewModelScope.launch {
            syncLock.busy.collect { busy ->
                mutableState.value = mutableState.value.copy(isSyncing = busy)
            }
        }
    }

    fun accept(intent: WearDashboardIntent) {
        if (intent is WearDashboardIntent.Open || intent == WearDashboardIntent.Back) {
            sensorJob?.cancel()
            sensorJob = null
        }
        mutableState.value = WearDashboardReducer.reduce(mutableState.value, intent)
        intent.toPreferenceIntent()?.let {
            updatePreference(it)
            return
        }
        when (intent) {
            is WearDashboardIntent.Open -> handleDestination(intent.destination)
            WearDashboardIntent.StartRecording -> requestRecordingStart()
            is WearDashboardIntent.PermissionsResolved -> handlePermissionResult(intent.granted)
            WearDashboardIntent.StopRecording -> recordingControl.stop().showFailure()
            is WearDashboardIntent.ObserveSensor -> observeLiveSensor(intent.sensorType)
            WearDashboardIntent.DismissFailure -> appFailures.dismiss()
            else -> Unit
        }
    }

    private fun WearDashboardIntent.toPreferenceIntent(): AppPreferencesIntent? = when (this) {
        is WearDashboardIntent.SetSamplingPeriod -> AppPreferencesIntent.SetSensorSamplingPeriod(index)

        WearDashboardIntent.ToggleBatteryRestriction -> AppPreferencesIntent.SetStopOnLowBattery(
            !mutableState.value.preferences.recording.stopRecordingOnLowBattery,
        )

        WearDashboardIntent.ToggleWakeLock ->
            AppPreferencesIntent.SetWakeLock(!mutableState.value.preferences.recording.useWakeLock)

        WearDashboardIntent.ToggleDisplay ->
            AppPreferencesIntent.SetKeepWearDisplayOn(!mutableState.value.preferences.display.keepWearDisplayOn)

        else -> null
    }

    private fun handleDestination(destination: WearMenuDestination) {
        when (destination) {
            WearMenuDestination.PHONE_INFO -> mutableEffects.trySend(WearDashboardEffect.OpenPhone)
            WearMenuDestination.PRIVACY, WearMenuDestination.TERMS -> openUrl(destination)
            else -> Unit
        }
    }

    private fun openUrl(destination: WearMenuDestination) {
        mutableEffects.trySend(WearDashboardEffect.OpenUrl(destination))
    }

    private fun requestRecordingStart() {
        val state = mutableState.value
        if (state.selectedSensorIds.isEmpty() && !state.includesGps) {
            mutableState.value = state.copy(message = WearDashboardMessage.PickSource)
            return
        }
        val missing = permissionUseCase(state.includesGps)
        if (missing.isEmpty()) startRecording() else requestPermissions(missing)
    }

    private fun requestPermissions(permissions: Set<String>) {
        mutableState.value = mutableState.value.copy(isWaitingForPermissions = true)
        mutableEffects.trySend(WearDashboardEffect.RequestPermissions(permissions))
    }

    private fun handlePermissionResult(granted: Boolean) {
        if (granted && mutableState.value.isWaitingForPermissions) startRecording()
        if (!granted) mutableState.value = mutableState.value.copy(message = WearDashboardMessage.PermissionRequired)
        mutableState.value = mutableState.value.copy(isWaitingForPermissions = false)
    }

    private fun startRecording() {
        val state = mutableState.value
        recordingControl.start(state.selectedSensorIds, state.includesGps, state.preferences).showFailure()
    }

    private fun observeLiveSensor(sensorType: Int) {
        sensorJob?.cancel()
        sensorJob = viewModelScope.launch {
            observeSensorValues(sensorType)
                .catch { error ->
                    appFailures.show(AppError.from(AppErrorCode.RECORDING, "Observe live sensor", error))
                    showSensorError()
                }
                .collect(::publishSensorValue)
        }
    }

    private fun publishSensorValue(value: List<Float>) {
        val samples = (mutableState.value.liveSamples + listOf(value)).takeLast(MAX_SAMPLES)
        mutableState.value = mutableState.value.copy(liveSamples = samples, message = null)
    }

    private fun showSensorError() {
        mutableState.value = mutableState.value.copy(message = WearDashboardMessage.SensorUnavailable)
    }

    private fun observePreferences() = viewModelScope.launch {
        preferencesRepository.preferences.collect { result ->
            result.fold(
                onSuccess = { preferences ->
                    mutableState.value = mutableState.value.copy(preferences = preferences)
                },
                onFailure = ::showOperationFailure,
            )
        }
    }

    private fun observeSession() = viewModelScope.launch {
        sessionStore.state.collect { session ->
            mutableState.value = WearDashboardReducer.recordingSessionChanged(mutableState.value, session)
        }
    }

    private fun observeRecordingFailures() = viewModelScope.launch {
        sessionStore.events.collect { event ->
            event.result.errorOrNull()?.let(appFailures::show)
        }
    }

    private fun updatePreference(intent: AppPreferencesIntent) {
        viewModelScope.launch { preferencesRepository.dispatch(intent).showFailure() }
    }

    private fun AppResult<*>.showFailure() {
        errorOrNull()?.let(::showOperationFailure)
    }

    private fun showOperationFailure(error: AppError) {
        appFailures.show(error)
    }

    private fun observeFailures() = viewModelScope.launch {
        appFailures.visibleFailure.collect { failure ->
            mutableState.value = mutableState.value.copy(visibleFailureCode = failure?.code)
        }
    }

    fun reportFailure(error: AppError) {
        appFailures.show(error)
    }

    private companion object {
        const val MAX_SAMPLES = 60
    }
}
