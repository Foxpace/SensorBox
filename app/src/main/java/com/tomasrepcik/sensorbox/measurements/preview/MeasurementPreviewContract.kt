package com.tomasrepcik.sensorbox.measurements.preview

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileContent
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileSummary
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementMetadataEntry

data class MeasurementPreviewState(
    val file: MeasurementFileSummary? = null,
    val content: MeasurementFileContent? = null,
    val sensorMetadata: List<MeasurementMetadataEntry> = emptyList(),
    val sensorChartTimeWindow: SensorChartTimeWindow = SensorChartTimeWindow(),
    val isLoading: Boolean = false,
    val errorCode: AppErrorCode? = null,
)

data class SensorChartTimeWindow(val startFraction: Float = 0f, val endFraction: Float = 1f) {
    val visibleFraction: Float get() = endFraction - startFraction
}

sealed interface MeasurementPreviewIntent {
    data class Load(val measurementId: String, val fileId: String) : MeasurementPreviewIntent

    data class Show(
        val file: MeasurementFileSummary,
        val content: MeasurementFileContent,
        val sensorMetadata: List<MeasurementMetadataEntry>,
    ) : MeasurementPreviewIntent

    data class ZoomSensorChartTimeWindow(val zoomFactor: Float, val focalPointFraction: Float) :
        MeasurementPreviewIntent

    data class ShiftSensorChartTimeWindow(val visibleWindowFraction: Float) : MeasurementPreviewIntent
    data object ShowEntireSensorChartTimeRange : MeasurementPreviewIntent
}

internal object MeasurementPreviewReducer {
    fun startLoading(state: MeasurementPreviewState): MeasurementPreviewState = state.copy(
        isLoading = true,
        errorCode = null,
    )

    fun show(state: MeasurementPreviewState, intent: MeasurementPreviewIntent.Show): MeasurementPreviewState =
        state.copy(
            file = intent.file,
            content = intent.content,
            sensorMetadata = intent.sensorMetadata,
            sensorChartTimeWindow = SensorChartTimeWindow(),
            isLoading = false,
            errorCode = null,
        )

    fun failLoading(state: MeasurementPreviewState, errorCode: AppErrorCode): MeasurementPreviewState = state.copy(
        isLoading = false,
        errorCode = errorCode,
    )

    fun zoomSensorChartTimeWindow(
        state: MeasurementPreviewState,
        zoomFactor: Float,
        focalPointFraction: Float,
    ): MeasurementPreviewState {
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
        state: MeasurementPreviewState,
        visibleWindowFraction: Float,
    ): MeasurementPreviewState {
        if (!visibleWindowFraction.isFinite()) return state
        val currentWindow = state.sensorChartTimeWindow
        val visibleFraction = currentWindow.visibleFraction.coerceIn(MINIMUM_VISIBLE_TIME_FRACTION, 1f)
        val nextStart = (currentWindow.startFraction + visibleFraction * visibleWindowFraction)
            .coerceIn(0f, 1f - visibleFraction)
        return state.copy(
            sensorChartTimeWindow = SensorChartTimeWindow(nextStart, nextStart + visibleFraction),
        )
    }

    fun showEntireSensorChartTimeRange(state: MeasurementPreviewState): MeasurementPreviewState =
        state.copy(sensorChartTimeWindow = SensorChartTimeWindow())

    private const val MINIMUM_VISIBLE_TIME_FRACTION = 0.01f
}
