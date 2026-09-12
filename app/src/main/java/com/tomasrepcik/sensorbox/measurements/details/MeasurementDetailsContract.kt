package com.tomasrepcik.sensorbox.measurements.details

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementDetails

data class MeasurementDetailsState(
    val details: MeasurementDetails? = null,
    val isLoading: Boolean = false,
    val errorCode: AppErrorCode? = null,
)

sealed interface MeasurementDetailsIntent {
    data class Load(val measurementId: String) : MeasurementDetailsIntent
    data class OpenFile(val fileId: String) : MeasurementDetailsIntent
}

sealed interface MeasurementDetailsEffect {
    data class OpenFile(val measurementId: String, val fileId: String) : MeasurementDetailsEffect
}

internal sealed interface MeasurementDetailsResult {
    data object Loading : MeasurementDetailsResult
    data class Loaded(val details: MeasurementDetails) : MeasurementDetailsResult
    data class LoadFailed(val errorCode: AppErrorCode) : MeasurementDetailsResult
}

internal object MeasurementDetailsReducer {
    fun reduce(result: MeasurementDetailsResult): MeasurementDetailsState = when (result) {
        MeasurementDetailsResult.Loading -> MeasurementDetailsState(isLoading = true)
        is MeasurementDetailsResult.Loaded -> MeasurementDetailsState(details = result.details)
        is MeasurementDetailsResult.LoadFailed -> MeasurementDetailsState(errorCode = result.errorCode)
    }
}
