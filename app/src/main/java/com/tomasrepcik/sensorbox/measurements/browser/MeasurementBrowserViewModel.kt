package com.tomasrepcik.sensorbox.measurements.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementRepository
import com.tomasrepcik.sensorbox.navigation.MainRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementBrowserViewModel @Inject constructor(
    private val repository: MeasurementRepository,
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MeasurementBrowserState())
    private val mutableEffects = Channel<MeasurementBrowserEffect>(Channel.BUFFERED)

    val state = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    fun onIntent(intent: MeasurementBrowserIntent) {
        when (intent) {
            MeasurementBrowserIntent.RefreshMeasurements -> refreshMeasurements()
            is MeasurementBrowserIntent.OpenMeasurementDetails -> openMeasurementDetails(intent.measurementId)
            is MeasurementBrowserIntent.OpenMeasurementFile -> openMeasurementFile(intent.fileId)
            is MeasurementBrowserIntent.ZoomSensorChartTimeWindow -> zoomSensorChartTimeWindow(intent)
            is MeasurementBrowserIntent.ShiftSensorChartTimeWindow -> shiftSensorChartTimeWindow(intent)
            MeasurementBrowserIntent.ShowEntireSensorChartTimeRange -> showEntireSensorChartTimeRange()
        }
    }

    private fun zoomSensorChartTimeWindow(intent: MeasurementBrowserIntent.ZoomSensorChartTimeWindow) {
        mutableState.value = MeasurementBrowserReducer.zoomSensorChartTimeWindow(
            state.value,
            intent.zoomFactor,
            intent.focalPointFraction,
        )
    }

    private fun shiftSensorChartTimeWindow(intent: MeasurementBrowserIntent.ShiftSensorChartTimeWindow) {
        mutableState.value = MeasurementBrowserReducer.shiftSensorChartTimeWindow(
            state.value,
            intent.visibleWindowFraction,
        )
    }

    private fun showEntireSensorChartTimeRange() {
        mutableState.value = MeasurementBrowserReducer.showEntireSensorChartTimeRange(state.value)
    }

    private fun refreshMeasurements() {
        viewModelScope.launch {
            mutableState.value = MeasurementBrowserReducer.reduce(
                state.value,
                MeasurementBrowserResult.LoadingMeasurements,
            )
            repository.loadMeasurements().fold(
                onSuccess = { measurements ->
                    mutableState.value = MeasurementBrowserReducer.reduce(
                        state.value,
                        MeasurementBrowserResult.MeasurementsLoaded(measurements),
                    )
                },
                onFailure = { error ->
                    mutableState.value = MeasurementBrowserReducer.reduce(
                        state.value,
                        MeasurementBrowserResult.MeasurementsLoadFailed(error.code),
                    )
                    appFailures.show(error)
                },
            )
        }
    }

    private fun openMeasurementDetails(measurementId: String) {
        viewModelScope.launch {
            mutableState.value = MeasurementBrowserReducer.reduce(
                state.value,
                MeasurementBrowserResult.LoadingMeasurementDetails,
            )
            repository.loadMeasurementDetails(measurementId).fold(
                onSuccess = { details ->
                    mutableState.value = MeasurementBrowserReducer.reduce(
                        state.value,
                        MeasurementBrowserResult.MeasurementDetailsLoaded(details),
                    )
                    mutableEffects.trySend(MeasurementBrowserEffect.Navigate(MainRoute.MEASUREMENT_DETAILS))
                },
                onFailure = { error ->
                    mutableState.value = MeasurementBrowserReducer.reduce(
                        state.value,
                        MeasurementBrowserResult.MeasurementDetailsLoadFailed(error.code),
                    )
                    appFailures.show(error)
                },
            )
        }
    }

    private fun openMeasurementFile(fileId: String) {
        val details = state.value.selectedMeasurement ?: return
        val file = details.files.firstOrNull { it.id == fileId } ?: return
        viewModelScope.launch {
            mutableState.value = MeasurementBrowserReducer.reduce(
                state.value,
                MeasurementBrowserResult.LoadingMeasurementFile,
            )
            repository.loadMeasurementFile(details.summary.id, fileId).fold(
                onSuccess = { content ->
                    mutableState.value = MeasurementBrowserReducer.reduce(
                        state.value,
                        MeasurementBrowserResult.MeasurementFileLoaded(file, content),
                    )
                    mutableEffects.trySend(MeasurementBrowserEffect.Navigate(MainRoute.MEASUREMENT_FILE))
                },
                onFailure = { error ->
                    mutableState.value = MeasurementBrowserReducer.reduce(
                        state.value,
                        MeasurementBrowserResult.MeasurementFileLoadFailed(error.code),
                    )
                    appFailures.show(error)
                },
            )
        }
    }
}
