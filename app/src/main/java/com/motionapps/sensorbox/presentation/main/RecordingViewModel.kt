package com.motionapps.sensorbox.presentation.main

import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.suspendFlatMap
import com.motionapps.sensorbox.core.preferences.AppPreferencesIntent
import com.motionapps.sensorbox.core.preferences.AppPreferencesRepository
import com.motionapps.sensorbox.domain.measurement.MeasurementRequest
import com.motionapps.sensorbox.domain.measurement.RecordingWorkflowGateway
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
import com.motionapps.sensorbox.domain.sensors.WearSensorCatalogStore
import com.motionapps.sensorservices.session.MeasurementSessionState
import com.motionapps.sensorservices.session.MeasurementSessionStore
import com.motionapps.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.motionapps.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.motionapps.wearoslib.connectivity.ObserveWearCapabilityUseCase
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import com.motionapps.wearoslib.connectivity.WearConnection
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
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
    private val workflow: RecordingWorkflowGateway,
    private val sessionStore: MeasurementSessionStore,
    private val observeWearCapability: ObserveWearCapabilityUseCase,
    private val sendWearMessage: SendWearMessageUseCase,
    private val wearSensorCatalog: WearSensorCatalogStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState())
    private val mutableEffects = Channel<RecordingEffect>(Channel.BUFFERED)
    private var elapsedJob: Job? = null

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

            is RecordingIntent.AddAnnotation -> workflow.annotate(intent.text).showFailure()

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
        val persisted = workflow.persistStorage(resultIntent)
        mutableState.value = state.value.copy(
            storagePath = workflow.storagePath(),
            message = if (persisted.isSuccess) RecordingMessage.NONE else RecordingMessage.STORAGE_REQUIRED,
            errorCode = persisted.errorOrNull()?.code,
        )
    }

    fun handlePermissionResult() {
        val request = state.value.toMeasurementRequest()
        val missing = workflow.missingPermissions(request, state.value.includesHeartRate())
        if (missing.isEmpty()) {
            startMeasurement()
        } else {
            showMessage(RecordingMessage.PERMISSION_REQUIRED, AppErrorCode.PERMISSION)
        }
    }

    private fun initialState() = RecordingState(
        sensors = workflow.sensors(),
        storagePath = workflow.storagePath(),
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
        mutableState.value = state.value.copy(session = session, elapsedSeconds = 0)
        if (session is MeasurementSessionState.Running) startElapsedTicker(session.startedAtElapsedRealtime)
    }

    private fun startElapsedTicker(startedAt: Long) {
        elapsedJob = viewModelScope.launch {
            while (isActive) {
                val seconds = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0) / 1_000L
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
                    mutableState.value = state.value.copy(isWearConnected = connection is WearConnection.Connected)
                    if (connection is WearConnection.Connected) {
                        WearCommandCodec.encode(WearCommand.RequestSensorList).suspendFlatMap { payload ->
                            sendWearMessage(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, payload)
                        }
                    } else {
                        wearSensorCatalog.clear()
                    }
                }
        }
    }

    private fun observeWearSensors() {
        viewModelScope.launch {
            wearSensorCatalog.sensors.collect { sensors ->
                mutableState.value = state.value.copy(
                    wearSensors = sensors.map { sensor ->
                        SensorDescriptor(
                            type = sensor.type,
                            name = sensor.name,
                            vendor = sensor.vendor,
                            isHeartRate = sensor.isHeartRate,
                        )
                    },
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
        val request = state.value.toMeasurementRequest()
        if (!request.hasAnySource()) {
            showMessage(RecordingMessage.PICK_AT_LEAST_ONE_SOURCE, AppErrorCode.VALIDATION)
            return
        }
        if (!workflow.hasStorage()) {
            reduce(RecordingIntent.ChooseStorage)
            showMessage(RecordingMessage.STORAGE_REQUIRED, AppErrorCode.STORAGE)
            return
        }
        requestMissingPermissions(request)?.let {
            mutableEffects.trySend(RecordingEffect.RequestPermissions(it))
            mutableState.value = state.value.copy(errorCode = AppErrorCode.PERMISSION)
            return
        }
        startForegroundMeasurement(request)
    }

    private fun requestMissingPermissions(request: MeasurementRequest): Set<String>? {
        val permissions = workflow.missingPermissions(request, state.value.includesHeartRate())
        return permissions.takeIf(Set<String>::isNotEmpty)
    }

    private fun startForegroundMeasurement(request: MeasurementRequest) {
        viewModelScope.launch {
            val result = workflow.start(request)
            if (result.isFailure) showFailure(result.errorOrNull())
        }
    }

    private fun stopMeasurement() {
        viewModelScope.launch { workflow.stop().showFailure() }
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

    private fun showFailure(error: com.motionapps.sensorbox.core.error.AppError?) {
        mutableState.value = state.value.copy(
            message = RecordingMessage.MEASUREMENT_FAILED,
            errorCode = error?.code ?: AppErrorCode.UNKNOWN,
        )
    }

    private fun RecordingState.toMeasurementRequest() = MeasurementRequest(
        sensorIds = selectedSensorIds,
        includesGps = includesGps,
        samplingPeriodIndex = preferences.sensorSamplingPeriod,
        stopOnLowBattery = preferences.restrictMeasurementOnLowBattery,
        useWakeLock = preferences.useWakeLock,
        gpsIntervalSeconds = preferences.gpsIntervalSeconds,
        gpsMinDistanceMeters = preferences.gpsMinDistanceMeters,
        wearSensorIds = selectedWearSensorIds,
        wearIncludesGps = wearIncludesGps,
        customName = customMeasurementName,
        measurementType = measurementType,
        delaySeconds = startDelaySeconds,
        durationSeconds = if (measurementType == "TIMED") durationSeconds.coerceAtLeast(1) else 0,
        notes = notes.lines().map(String::trim).filter(String::isNotEmpty),
        alarmOffsetsSeconds = alarmOffsets.split(',', ';', ' ')
            .mapNotNull(String::toIntOrNull).filter { it >= 0 },
        activityRecognition = activityRecognition,
        activityRecognitionPeriodSeconds = activityRecognitionPeriodSeconds,
        significantMotion = significantMotion,
    )

    private fun RecordingState.includesHeartRate(): Boolean = sensors.any {
        it.isHeartRate && it.type in selectedSensorIds
    }

    private fun MeasurementRequest.hasAnySource(): Boolean = sensorIds.isNotEmpty() || includesGps ||
        wearSensorIds.isNotEmpty() || wearIncludesGps || activityRecognition || significantMotion
}
