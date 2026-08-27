package com.tomasrepcik.sensorbox.presentation.main

import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementDetails
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileContent
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileSummary
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementSummary

data class MeasurementBrowserState(
    val measurements: List<MeasurementSummary> = emptyList(),
    val selectedMeasurement: MeasurementDetails? = null,
    val selectedFile: MeasurementFileSummary? = null,
    val selectedFileContent: MeasurementFileContent? = null,
    val sensorChartTimeWindow: SensorChartTimeWindow = SensorChartTimeWindow(),
    val isLoading: Boolean = false,
    val errorCode: AppErrorCode? = null,
)

data class SensorChartTimeWindow(val startFraction: Float = 0f, val endFraction: Float = 1f) {
    val visibleFraction: Float get() = endFraction - startFraction
}

sealed interface MeasurementBrowserIntent {
    data object RefreshMeasurements : MeasurementBrowserIntent
    data class OpenMeasurementDetails(val measurementId: String) : MeasurementBrowserIntent
    data class OpenMeasurementFile(val fileId: String) : MeasurementBrowserIntent
    data class ZoomSensorChartTimeWindow(val zoomFactor: Float, val focalPointFraction: Float) :
        MeasurementBrowserIntent

    data class ShiftSensorChartTimeWindow(val visibleWindowFraction: Float) : MeasurementBrowserIntent
    data object ShowEntireSensorChartTimeRange : MeasurementBrowserIntent
}

sealed interface MeasurementBrowserEffect {
    data class Navigate(val route: MainRoute) : MeasurementBrowserEffect
}

internal sealed interface MeasurementBrowserResult {
    data object LoadingMeasurements : MeasurementBrowserResult
    data object LoadingMeasurementDetails : MeasurementBrowserResult
    data object LoadingMeasurementFile : MeasurementBrowserResult
    data class MeasurementsLoaded(val measurements: List<MeasurementSummary>) : MeasurementBrowserResult
    data class MeasurementDetailsLoaded(val details: MeasurementDetails) : MeasurementBrowserResult
    data class MeasurementFileLoaded(val file: MeasurementFileSummary, val content: MeasurementFileContent) :
        MeasurementBrowserResult

    data class MeasurementsLoadFailed(val errorCode: AppErrorCode) : MeasurementBrowserResult
    data class MeasurementDetailsLoadFailed(val errorCode: AppErrorCode) : MeasurementBrowserResult
    data class MeasurementFileLoadFailed(val errorCode: AppErrorCode) : MeasurementBrowserResult
}

internal object MeasurementBrowserReducer {
    fun reduce(state: MeasurementBrowserState, result: MeasurementBrowserResult): MeasurementBrowserState =
        when (result) {
            MeasurementBrowserResult.LoadingMeasurements,
            MeasurementBrowserResult.LoadingMeasurementDetails,
            MeasurementBrowserResult.LoadingMeasurementFile,
            -> state.copy(isLoading = true, errorCode = null)

            is MeasurementBrowserResult.MeasurementsLoaded -> state.copy(
                measurements = result.measurements,
                isLoading = false,
                errorCode = null,
            )

            is MeasurementBrowserResult.MeasurementDetailsLoaded -> state.copy(
                selectedMeasurement = result.details,
                selectedFile = null,
                selectedFileContent = null,
                isLoading = false,
                errorCode = null,
            )

            is MeasurementBrowserResult.MeasurementFileLoaded -> state.copy(
                selectedFile = result.file,
                selectedFileContent = result.content,
                sensorChartTimeWindow = SensorChartTimeWindow(),
                isLoading = false,
                errorCode = null,
            )

            is MeasurementBrowserResult.MeasurementsLoadFailed -> state.copy(
                isLoading = false,
                errorCode = result.errorCode,
            )

            is MeasurementBrowserResult.MeasurementDetailsLoadFailed -> state.copy(
                isLoading = false,
                errorCode = result.errorCode,
            )

            is MeasurementBrowserResult.MeasurementFileLoadFailed -> state.copy(
                isLoading = false,
                errorCode = result.errorCode,
            )
        }

    fun zoomSensorChartTimeWindow(
        state: MeasurementBrowserState,
        zoomFactor: Float,
        focalPointFraction: Float,
    ): MeasurementBrowserState {
        if (!zoomFactor.isFinite() || zoomFactor <= 0f) return state
        val currentWindow = state.sensorChartTimeWindow
        val currentVisibleFraction = currentWindow.visibleFraction.coerceIn(MINIMUM_VISIBLE_TIME_FRACTION, 1f)
        val nextVisibleFraction = (currentVisibleFraction / zoomFactor)
            .coerceIn(MINIMUM_VISIBLE_TIME_FRACTION, 1f)
        val focalFraction = focalPointFraction.coerceIn(0f, 1f)
        val focalPosition = currentWindow.startFraction + currentVisibleFraction * focalFraction
        val nextStart = (focalPosition - nextVisibleFraction * focalFraction)
            .coerceIn(0f, 1f - nextVisibleFraction)
        return state.copy(
            sensorChartTimeWindow = SensorChartTimeWindow(nextStart, nextStart + nextVisibleFraction),
        )
    }

    fun shiftSensorChartTimeWindow(
        state: MeasurementBrowserState,
        visibleWindowFraction: Float,
    ): MeasurementBrowserState {
        if (!visibleWindowFraction.isFinite()) return state
        val currentWindow = state.sensorChartTimeWindow
        val visibleFraction = currentWindow.visibleFraction.coerceIn(MINIMUM_VISIBLE_TIME_FRACTION, 1f)
        val nextStart = (currentWindow.startFraction + visibleFraction * visibleWindowFraction)
            .coerceIn(0f, 1f - visibleFraction)
        return state.copy(
            sensorChartTimeWindow = SensorChartTimeWindow(nextStart, nextStart + visibleFraction),
        )
    }

    fun showEntireSensorChartTimeRange(state: MeasurementBrowserState): MeasurementBrowserState =
        state.copy(sensorChartTimeWindow = SensorChartTimeWindow())

    private const val MINIMUM_VISIBLE_TIME_FRACTION = 0.01f
}
