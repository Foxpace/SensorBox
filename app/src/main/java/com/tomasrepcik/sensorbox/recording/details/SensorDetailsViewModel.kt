package com.tomasrepcik.sensorbox.recording.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.preview.DevicePreviewRepository
import com.tomasrepcik.sensorbox.recording.sources.AvailableRecordingSourcesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SensorDetailsViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val availableSources: AvailableRecordingSourcesUseCase,
    private val devicePreview: DevicePreviewRepository,
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState())
    private val mutableEffects = Channel<SensorDetailsEffect>(Channel.BUFFERED)
    private var previewJob: Job? = null

    val state: StateFlow<RecordingState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        observePreferences()
        observeAvailableSources()
    }

    fun accept(intent: SensorDetailsIntent) {
        when (intent) {
            SensorDetailsIntent.StartGpsPreview -> startGpsPreview()

            SensorDetailsIntent.StopPreview -> stopPreview()

            SensorDetailsIntent.RequestLocationPreviewPermission ->
                mutableEffects.trySend(SensorDetailsEffect.RequestLocationPreviewPermission)

            SensorDetailsIntent.OpenSensorPreview -> mutableEffects.trySend(
                SensorDetailsEffect.OpenSensorPreview(state.value.detailsSensorType, state.value.detailsDevice),
            )

            else -> mutableState.value = SensorDetailsReducer.reduce(state.value, intent)
        }
    }

    fun handleLocationPreviewPermissionResult() {
        startGpsPreview()
    }

    fun reportFailure(error: AppError) {
        appFailures.show(error)
    }

    private fun initialState() = RecordingState(
        sensors = availableSources.current.phoneSensors,
        watchSensors = availableSources.current.watchSensors,
        isWatchConnected = availableSources.current.isWatchConnected,
    )

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

    private fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { result ->
                result.fold(
                    onSuccess = { preferences -> mutableState.value = state.value.copy(preferences = preferences) },
                    onFailure = appFailures::show,
                )
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
                    watchSensors = sources.watchSensors,
                    isWatchConnected = sources.isWatchConnected,
                )
            }
        }
    }
}
