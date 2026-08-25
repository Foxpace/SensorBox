package com.tomasrepcik.sensorbox.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionapps.sensorservices.session.MeasurementSessionState
import com.motionapps.sensorservices.session.MeasurementSessionStore
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesIntent
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.domain.measurement.WearMeasurementControlUseCase
import com.tomasrepcik.sensorbox.domain.measurement.WearMeasurementPermissionUseCase
import com.tomasrepcik.sensorbox.domain.sensors.GetWearSensorsUseCase
import com.tomasrepcik.sensorbox.domain.sensors.ObserveSensorValuesUseCase
import com.tomasrepcik.sensorbox.domain.sync.SyncWearMeasurementsUseCase
import com.tomasrepcik.sensorbox.presentation.menu.WearMenuDestination
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
    getSensors: GetWearSensorsUseCase,
    private val observeSensorValues: ObserveSensorValuesUseCase,
    private val permissionUseCase: WearMeasurementPermissionUseCase,
    private val measurementControl: WearMeasurementControlUseCase,
    private val preferencesRepository: AppPreferencesRepository,
    private val sessionStore: MeasurementSessionStore,
    private val syncMeasurements: SyncWearMeasurementsUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WearDashboardState(sensors = getSensors()))
    private val mutableEffects = Channel<WearDashboardEffect>(Channel.BUFFERED)
    private var sensorJob: Job? = null
    private var pendingStart = false

    val state = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()
    val chartModelProducer = CartesianChartModelProducer()

    init {
        observePreferences()
        observeSession()
    }

    fun accept(intent: WearDashboardIntent) {
        mutableState.value = WearDashboardReducer.reduce(mutableState.value, intent)
        when (intent) {
            is WearDashboardIntent.Open -> handleDestination(intent.destination)
            WearDashboardIntent.StartMeasurement -> requestMeasurementStart()
            is WearDashboardIntent.PermissionsResolved -> handlePermissionResult(intent.granted)
            WearDashboardIntent.StopMeasurement -> measurementControl.stop().showFailure()
            is WearDashboardIntent.ObserveSensor -> observeLiveSensor(intent.sensorType)
            is WearDashboardIntent.SetSamplingPeriod -> setSamplingPeriod(intent.index)
            WearDashboardIntent.ToggleBatteryRestriction -> toggleBatteryRestriction()
            WearDashboardIntent.ToggleWakeLock -> toggleWakeLock()
            WearDashboardIntent.ToggleDisplay -> toggleDisplay()
            WearDashboardIntent.SyncMeasurements -> startSync()
            else -> Unit
        }
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

    private fun requestMeasurementStart() {
        val state = mutableState.value
        if (state.selectedSensorIds.isEmpty() && !state.includesGps) {
            mutableState.value = state.copy(message = WearDashboardMessage.PickSource)
            return
        }
        val missing = permissionUseCase(state.includesGps)
        if (missing.isEmpty()) startMeasurement() else requestPermissions(missing)
    }

    private fun requestPermissions(permissions: Set<String>) {
        pendingStart = true
        mutableEffects.trySend(WearDashboardEffect.RequestPermissions(permissions))
    }

    private fun handlePermissionResult(granted: Boolean) {
        if (granted && pendingStart) startMeasurement()
        if (!granted) mutableState.value = mutableState.value.copy(message = WearDashboardMessage.PermissionRequired)
        pendingStart = false
    }

    private fun startMeasurement() {
        val state = mutableState.value
        measurementControl.start(state.selectedSensorIds, state.includesGps, state.preferences).showFailure()
    }

    private fun observeLiveSensor(sensorType: Int) {
        sensorJob?.cancel()
        sensorJob = viewModelScope.launch {
            observeSensorValues(sensorType)
                .catch { error ->
                    AppError.from(AppErrorCode.MEASUREMENT, "Observe live sensor", error)
                    showSensorError()
                }
                .collect(::publishSensorValue)
        }
    }

    private suspend fun publishSensorValue(value: Float) {
        val samples = sampleBuffer + value
        sampleBuffer = samples.takeLast(MAX_SAMPLES)
        mutableState.value = mutableState.value.copy(latestValue = value, message = null)
        chartModelProducer.runTransaction { lineModel { series(sampleBuffer) } }
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
                onFailure = { showOperationFailure() },
            )
        }
    }

    private fun observeSession() = viewModelScope.launch {
        sessionStore.state.collect { session ->
            val route = if (session is MeasurementSessionState.Running) WearRoute.ACTIVE else null
            if (route != null) mutableState.value = mutableState.value.copy(route = route)
            if (session is MeasurementSessionState.Idle && mutableState.value.route == WearRoute.ACTIVE) {
                mutableState.value = mutableState.value.copy(route = WearRoute.MENU)
            }
        }
    }

    private fun updatePreference(intent: AppPreferencesIntent) {
        viewModelScope.launch { preferencesRepository.dispatch(intent).showFailure() }
    }

    private fun setSamplingPeriod(index: Int) = updatePreference(
        AppPreferencesIntent.SetSensorSamplingPeriod(index),
    )

    private fun toggleBatteryRestriction() = updatePreference(
        AppPreferencesIntent.SetLowBatteryRestriction(
            !mutableState.value.preferences.recording.restrictMeasurementOnLowBattery,
        ),
    )

    private fun toggleWakeLock() = updatePreference(
        AppPreferencesIntent.SetWakeLock(!mutableState.value.preferences.recording.useWakeLock),
    )

    private fun toggleDisplay() = updatePreference(
        AppPreferencesIntent.SetKeepWearDisplayOn(!mutableState.value.preferences.display.keepWearDisplayOn),
    )

    private fun startSync() {
        if (mutableState.value.isSyncing) return
        mutableState.value = mutableState.value.copy(isSyncing = true, message = WearDashboardMessage.Syncing)
        viewModelScope.launch {
            val result = syncMeasurements()
            val message = result.fold(
                onSuccess = WearDashboardMessage::FilesSent,
                onFailure = { WearDashboardMessage.SyncFailed },
            )
            mutableState.value = mutableState.value.copy(isSyncing = false, message = message)
        }
    }

    private var sampleBuffer: List<Float> = emptyList()

    private fun AppResult<*>.showFailure() {
        if (isFailure) showOperationFailure()
    }

    private fun showOperationFailure() {
        mutableState.value = mutableState.value.copy(message = WearDashboardMessage.SyncFailed)
    }

    private companion object {
        const val MAX_SAMPLES = 60
    }
}
