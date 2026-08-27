package com.tomasrepcik.sensorbox.presentation.main

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesIntent
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.domain.measurement.DocumentStorageGateway
import com.tomasrepcik.sensorbox.domain.measurement.MeasurementPermissionsUseCase
import com.tomasrepcik.sensorbox.domain.measurement.MeasurementRequest
import com.tomasrepcik.sensorbox.domain.measurement.RecordingControlUseCase
import com.tomasrepcik.sensorbox.domain.sensors.AvailableSensorsUseCase
import com.tomasrepcik.sensorbox.domain.sensors.WearSensorCatalogStore
import com.tomasrepcik.sensorbox.domain.sensors.toSensorDescriptor
import com.tomasrepcik.sensorbox.sensorservices.session.MeasurementSessionState
import com.tomasrepcik.sensorbox.sensorservices.session.MeasurementSessionStore
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.connectivity.ObserveWearCapabilityUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions")
class RecordingViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val availableSensors: AvailableSensorsUseCase,
    private val storage: DocumentStorageGateway,
    private val permissions: MeasurementPermissionsUseCase,
    private val recording: RecordingControlUseCase,
    private val sessionStore: MeasurementSessionStore,
    private val observeWearCapability: ObserveWearCapabilityUseCase,
    private val sendWearCommand: SendWearCommandUseCase,
    private val wearSensorCatalog: WearSensorCatalogStore,
    private val elapsedRealtimeClock: ElapsedRealtimeClock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState())
    private val mutableEffects = Channel<RecordingEffect>(Channel.BUFFERED)
    private var elapsedJob: Job? = null
    private var startJob: Job? = null

    val state: StateFlow<RecordingState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        observePreferences()
        observeMeasurementSession()
        observeWearConnection()
        observeWearSensors()
    }

    fun accept(intent: RecordingIntent) {
        when (intent) {
            RecordingIntent.StartMeasurement -> startMeasurement()

            RecordingIntent.StopMeasurement -> stopMeasurement()

            is RecordingIntent.AddAnnotation -> recording.annotate(intent.text).showFailure()

            is RecordingIntent.SetSamplingPeriod -> updatePreference(
                AppPreferencesIntent.SetSensorSamplingPeriod(intent.index),
            )

            is RecordingIntent.SetLowBatteryRestriction -> updatePreference(
                AppPreferencesIntent.SetLowBatteryRestriction(intent.enabled),
            )

            is RecordingIntent.SetWakeLock -> updatePreference(AppPreferencesIntent.SetWakeLock(intent.enabled))

            is RecordingIntent.SetKeepScreenAwake -> updatePreference(
                AppPreferencesIntent.SetKeepPhoneDisplayOn(intent.enabled),
            )

            is RecordingIntent.SetGpsInterval -> updatePreference(AppPreferencesIntent.SetGpsInterval(intent.seconds))

            is RecordingIntent.SetGpsDistance -> updatePreference(AppPreferencesIntent.SetGpsMinDistance(intent.meters))

            else -> reduce(intent)
        }
    }

    fun handleStorageResult(resultIntent: Intent?) {
        val persisted = resultIntent?.let(storage::persist) ?: AppResult.failure(
            AppError(AppErrorCode.STORAGE, "Select recording storage directory"),
        )
        mutableState.value = state.value.copy(
            storagePath = storage.displayPath().getOrNull(),
            message = if (persisted.isSuccess) RecordingMessage.NONE else RecordingMessage.STORAGE_REQUIRED,
            errorCode = persisted.errorOrNull()?.code,
        )
    }

    fun handlePermissionResult() {
        val request = state.value.toMeasurementRequest()
        val missing = permissions.missingPermissions(request)
        if (missing.isEmpty()) {
            startMeasurement()
        } else {
            showMessage(RecordingMessage.PERMISSION_REQUIRED, AppErrorCode.PERMISSION)
        }
    }

    private fun initialState() = RecordingState(
        sensors = availableSensors(),
        storagePath = storage.displayPath().getOrNull(),
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
                        mutableState.value = state.value.copy(
                            message = RecordingMessage.MEASUREMENT_FAILED,
                            errorCode = error.code,
                        )
                    },
                )
            }
        }
    }

    private fun observeMeasurementSession() {
        viewModelScope.launch {
            sessionStore.state.collect { session ->
                onSessionChanged(session)
            }
        }
    }

    private fun onSessionChanged(session: MeasurementSessionState) {
        elapsedJob?.cancel()
        mutableState.value = RecordingReducer.measurementSessionChanged(state.value, session)
        if (session is MeasurementSessionState.Running) startElapsedTicker(session.startedAtElapsedRealtime)
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

    private fun observeWearConnection() {
        viewModelScope.launch {
            observeWearCapability(WEAR_APP_CAPABILITY)
                .catch { error ->
                    AppError.from(AppErrorCode.CONNECTIVITY, "Observe Wear connection", error)
                    emit(WearConnection.Disconnected)
                }
                .collect { connection ->
                    if (connection is WearConnection.Connected) {
                        sendWearCommand(
                            WEAR_APP_CAPABILITY,
                            WEAR_MESSAGE_PATH,
                            WearCommand.RequestAvailableSensors,
                        )
                    } else {
                        wearSensorCatalog.clear()
                    }
                }
        }
    }

    private fun observeWearSensors() {
        viewModelScope.launch {
            combine(wearSensorCatalog.sensors, wearSensorCatalog.isAvailable) { sensors, isAvailable ->
                sensors to isAvailable
            }.collect { (sensors, isAvailable) ->
                mutableState.value = state.value.copy(
                    isWearConnected = isAvailable,
                    wearSensors = sensors.map { sensor -> sensor.toSensorDescriptor() },
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
    private fun startMeasurement() {
        if (state.value.isStarting) return
        val request = state.value.toMeasurementRequest()
        if (!request.hasAnySource()) {
            showMessage(RecordingMessage.PICK_AT_LEAST_ONE_SOURCE, AppErrorCode.VALIDATION)
            return
        }
        if (storage.hasStorage().getOrNull() != true) {
            reduce(RecordingIntent.ChooseStorage)
            showMessage(RecordingMessage.STORAGE_REQUIRED, AppErrorCode.STORAGE)
            return
        }
        requestMissingPermissions(request)?.let {
            mutableEffects.trySend(RecordingEffect.RequestPermissions(it))
            mutableState.value = state.value.copy(errorCode = AppErrorCode.PERMISSION)
            return
        }
        startForegroundMeasurement(request, state.value.startDelaySeconds)
    }

    private fun requestMissingPermissions(request: MeasurementRequest): Set<String>? {
        val missingPermissions = permissions.missingPermissions(request)
        return missingPermissions.takeIf(Set<String>::isNotEmpty)
    }

    private fun startForegroundMeasurement(request: MeasurementRequest, countdownSeconds: Int) {
        mutableState.value = RecordingReducer.measurementStartRequested(state.value, countdownSeconds)
        startJob = viewModelScope.launch {
            waitForStartCountdown(countdownSeconds)
            val result = recording.start(request)
            if (result.isFailure) {
                mutableState.value = RecordingReducer.measurementStartFailed(state.value)
                showFailure(result.errorOrNull())
            }
        }
    }

    private suspend fun waitForStartCountdown(seconds: Int) {
        for (remaining in seconds downTo 1) {
            mutableState.value = RecordingReducer.measurementCountdownChanged(state.value, remaining)
            delay(1_000L)
        }
        mutableState.value = RecordingReducer.measurementCountdownChanged(state.value, null)
    }

    private fun stopMeasurement() {
        if (state.value.isStarting && state.value.session is MeasurementSessionState.Idle) {
            startJob?.cancel()
            startJob = null
            mutableState.value = RecordingReducer.measurementStartFailed(state.value)
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
        mutableState.value = state.value.copy(
            message = when {
                error?.code == AppErrorCode.PERMISSION && error.context["source"] == "wear" ->
                    RecordingMessage.WEAR_PERMISSION_REQUIRED

                error?.code == AppErrorCode.PERMISSION -> RecordingMessage.PERMISSION_REQUIRED

                else -> RecordingMessage.MEASUREMENT_FAILED
            },
            errorCode = error?.code ?: AppErrorCode.UNKNOWN,
        )
    }

    private fun MeasurementRequest.hasAnySource(): Boolean = sensorIds.isNotEmpty() || includesGps ||
        wearSensorIds.isNotEmpty() || wearIncludesGps || activityRecognition || significantMotion
}
