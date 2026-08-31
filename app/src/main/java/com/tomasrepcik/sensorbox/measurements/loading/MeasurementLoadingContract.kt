package com.tomasrepcik.sensorbox.measurements.loading

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileSummary
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementMetadataEntry

data class MeasurementLoadingRequest(
    val measurementId: String,
    val file: MeasurementFileSummary,
    val sensorMetadata: List<MeasurementMetadataEntry>,
)

data class MeasurementLoadingState(
    val request: MeasurementLoadingRequest? = null,
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val errorCode: AppErrorCode? = null,
)

sealed interface MeasurementLoadingIntent {
    data class Load(val measurementId: String, val fileId: String) : MeasurementLoadingIntent
    data object Cancel : MeasurementLoadingIntent
}

sealed interface MeasurementLoadingEffect {
    data class OpenPreview(val measurementId: String, val fileId: String) : MeasurementLoadingEffect
}

internal sealed interface MeasurementLoadingResult {
    data object Starting : MeasurementLoadingResult
    data class Loading(val request: MeasurementLoadingRequest) : MeasurementLoadingResult
    data class Progress(val progress: Float) : MeasurementLoadingResult
    data object Loaded : MeasurementLoadingResult
    data object Cancelled : MeasurementLoadingResult
    data class LoadFailed(val errorCode: AppErrorCode) : MeasurementLoadingResult
}

internal object MeasurementLoadingReducer {
    fun reduce(state: MeasurementLoadingState, result: MeasurementLoadingResult): MeasurementLoadingState =
        when (result) {
            MeasurementLoadingResult.Starting -> MeasurementLoadingState(isLoading = true)

            is MeasurementLoadingResult.Loading -> MeasurementLoadingState(
                request = result.request,
                isLoading = true,
            )

            is MeasurementLoadingResult.Progress -> state.copy(progress = result.progress.coerceIn(0f, 1f))

            MeasurementLoadingResult.Loaded -> state.copy(progress = 1f, isLoading = false)

            MeasurementLoadingResult.Cancelled -> state.copy(isLoading = false)

            is MeasurementLoadingResult.LoadFailed -> state.copy(
                isLoading = false,
                errorCode = result.errorCode,
            )
        }
}
