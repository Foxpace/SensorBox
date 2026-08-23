package com.motionapps.sensorbox.presentation.main

import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.suspendFlatMap
import com.motionapps.sensorbox.core.preferences.AppPreferencesIntent
import com.motionapps.sensorbox.core.preferences.AppPreferencesRepository
import com.motionapps.sensorbox.domain.measurement.DocumentStorageUseCase
import com.motionapps.sensorbox.domain.measurement.MeasurementControlUseCase
import com.motionapps.sensorbox.domain.measurement.MeasurementPermissionUseCase
import com.motionapps.sensorbox.domain.measurement.MeasurementRequest
import com.motionapps.sensorbox.domain.sensors.GetAvailableSensorsUseCase
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
import com.motionapps.sensorservices.session.MeasurementSessionState
import com.motionapps.sensorservices.session.MeasurementSessionStore
import com.motionapps.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.motionapps.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.motionapps.wearoslib.connectivity.ObserveWearCapabilityUseCase
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import com.motionapps.wearoslib.connectivity.WearConnection
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
import com.motionapps.wearoslib.protocol.WearSensorCatalogStore
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
class MainViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val getAvailableSensors: GetAvailableSensorsUseCase,
    private val documentStorage: DocumentStorageUseCase,
    private val measurementPermissions: MeasurementPermissionUseCase,
    private val measurementControl: MeasurementControlUseCase,
    private val sessionStore: MeasurementSessionStore,
    private val observeWearCapability: ObserveWearCapabilityUseCase,
    private val sendWearMessage: SendWearMessageUseCase,
    private val wearSensorCatalog: WearSensorCatalogStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState())
    private val mutableEffects = Channel<MainEffect>(Channel.BUFFERED)
    private var hasChosenInitialRoute = false
    private var elapsedJob: Job? = null

    val state: StateFlow<MainState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        observePreferences()
        observeMeasurementSession()
        observeWearConnection()
        observeWearSensors()
    }

    fun accept(intent: MainIntent) {
        when (intent) {
            MainIntent.CompleteOnboarding -> completeOnboarding()

            MainIntent.StartMeasurement -> startMeasurement()

            MainIntent.StopMeasurement -> measurementControl.stop().showFailure()

            is MainIntent.AddAnnotation -> measurementControl.annotate(intent.text).showFailure()

            is MainIntent.SetSamplingPeriod -> updatePreference(
                AppPreferencesIntent.SetSensorSamplingPeriod(intent.index),
            )

            is MainIntent.SetLowBatteryRestriction -> updatePreference(
                AppPreferencesIntent.SetLowBatteryRestriction(intent.enabled),
            )

            is MainIntent.SetWakeLock -> updatePreference(AppPreferencesIntent.SetWakeLock(intent.enabled))

            is MainIntent.SetKeepScreenAwake -> updatePreference(
                AppPreferencesIntent.SetKeepPhoneDisplayOn(intent.enabled),
            )

            is MainIntent.SetGpsInterval -> updatePreference(AppPreferencesIntent.SetGpsInterval(intent.seconds))

            is MainIntent.SetGpsDistance -> updatePreference(AppPreferencesIntent.SetGpsMinDistance(intent.meters))

            else -> reduce(intent)
        }
    }

    fun handleStorageResult(resultIntent: Intent?) {
        val persisted = resultIntent?.let(documentStorage::persist) ?: Result.failure(
            com.motionapps.sensorbox.core.error.AppError(
                com.motionapps.sensorbox.core.error.AppError.Kind.STORAGE,
                "Select storage directory",
            ),
        )
        mutableState.value = state.value.copy(
            storagePath = documentStorage.displayPath().getOrNull(),
            message = if (persisted.isSuccess) MainMessage.NONE else MainMessage.STORAGE_REQUIRED,
        )
    }

    fun handlePermissionResult() {
        val request = state.value.toMeasurementRequest()
        val missing = measurementPermissions.missingPermissions(request, state.value.includesHeartRate())
        if (missing.isEmpty()) startMeasurement() else showMessage(MainMessage.PERMISSION_REQUIRED)
    }

    fun showPrivacyRationale() {
        hasChosenInitialRoute = true
        reduce(MainIntent.Navigate(MainRoute.PRIVACY))
    }

    private fun initialState() = MainState(
        sensors = getAvailableSensors(),
        storagePath = documentStorage.displayPath().getOrNull(),
    )

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { result ->
                result.fold(
                    onSuccess = { preferences ->
                        val route = initialRoute(preferences.hasCompletedIntro, preferences.hasAcceptedPolicy)
                        mutableState.value = state.value.copy(
                            preferences = preferences,
                            hasLoadedPreferences = true,
                            route = route,
                        )
                    },
                    onFailure = {
                        mutableState.value = state.value.copy(
                            hasLoadedPreferences = true,
                            message = MainMessage.MEASUREMENT_FAILED,
                        )
                    },
                )
            }
        }
    }

    private fun initialRoute(completedIntro: Boolean, acceptedPolicy: Boolean): MainRoute {
        if (hasChosenInitialRoute) return state.value.route
        hasChosenInitialRoute = true
        return if (completedIntro && acceptedPolicy) MainRoute.RECORD else MainRoute.ONBOARDING
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
                    AppError.from(AppError.Kind.CONNECTIVITY, "Observe Wear connection", error)
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

    private fun reduce(intent: MainIntent) {
        val next = MainReducer.reduce(state.value, intent)
        mutableState.value = next.state
        next.effect?.let(mutableEffects::trySend)
    }

    private fun completeOnboarding() {
        if (!documentStorage.hasStorage().getOrElse {
                showMessage(MainMessage.STORAGE_REQUIRED)
                return
            }
        ) {
            showMessage(MainMessage.STORAGE_REQUIRED)
            return
        }
        viewModelScope.launch {
            preferencesRepository.dispatch(AppPreferencesIntent.AcceptPolicy).getOrElse {
                showMessage(MainMessage.MEASUREMENT_FAILED)
                return@launch
            }
            preferencesRepository.dispatch(AppPreferencesIntent.CompleteIntro).getOrElse {
                showMessage(MainMessage.MEASUREMENT_FAILED)
                return@launch
            }
            mutableState.value = state.value.copy(route = MainRoute.RECORD)
        }
    }

    @Suppress("ReturnCount")
    private fun startMeasurement() {
        val request = state.value.toMeasurementRequest()
        if (!request.hasAnySource()) {
            showMessage(MainMessage.PICK_AT_LEAST_ONE_SOURCE)
            return
        }
        if (!documentStorage.hasStorage().getOrElse {
                showMessage(MainMessage.STORAGE_REQUIRED)
                return
            }
        ) {
            reduce(MainIntent.ChooseStorage)
            showMessage(MainMessage.STORAGE_REQUIRED)
            return
        }
        requestMissingPermissions(request)?.let {
            mutableEffects.trySend(MainEffect.RequestPermissions(it))
            return
        }
        startForegroundMeasurement(request)
    }

    private fun requestMissingPermissions(request: MeasurementRequest): Set<String>? {
        val permissions = measurementPermissions.missingPermissions(request, state.value.includesHeartRate())
        return permissions.takeIf(Set<String>::isNotEmpty)
    }

    private fun startForegroundMeasurement(request: MeasurementRequest) {
        viewModelScope.launch {
            val result = measurementControl.start(request)
            if (result.isFailure) showMessage(MainMessage.MEASUREMENT_FAILED)
        }
    }

    private fun updatePreference(intent: AppPreferencesIntent) {
        viewModelScope.launch { preferencesRepository.dispatch(intent).showFailure() }
    }

    private fun showMessage(message: MainMessage) {
        mutableState.value = state.value.copy(message = message)
    }

    private fun Result<*>.showFailure() {
        if (isFailure) showMessage(MainMessage.MEASUREMENT_FAILED)
    }

    private fun MainState.toMeasurementRequest() = MeasurementRequest(
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

    private fun MainState.includesHeartRate(): Boolean = sensors.any {
        it.isHeartRate && it.type in selectedSensorIds
    }

    private fun MeasurementRequest.hasAnySource(): Boolean = sensorIds.isNotEmpty() || includesGps ||
        wearSensorIds.isNotEmpty() || wearIncludesGps || activityRecognition || significantMotion
}
