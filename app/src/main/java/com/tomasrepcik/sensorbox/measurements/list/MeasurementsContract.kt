package com.tomasrepcik.sensorbox.measurements.list

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementSummary

data class MeasurementsState(
    val measurements: List<MeasurementSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorCode: AppErrorCode? = null,
) {
    val recordings: List<List<MeasurementSummary>>
        get() = measurements.groupBy { measurement ->
            measurement.sessionId?.let { "session:$it" } ?: "measurement:${measurement.id}"
        }.values.map { group -> group.sortedBy { it.device == "watch" } }
}

sealed interface MeasurementsIntent {
    data object Refresh : MeasurementsIntent
    data class OpenDetails(val measurementId: String) : MeasurementsIntent
}

sealed interface MeasurementsEffect {
    data class OpenDetails(val measurementId: String) : MeasurementsEffect
}

internal sealed interface MeasurementsResult {
    data object Loading : MeasurementsResult
    data class Loaded(val measurements: List<MeasurementSummary>) : MeasurementsResult
    data class LoadFailed(val errorCode: AppErrorCode) : MeasurementsResult
}

internal object MeasurementsReducer {
    fun reduce(state: MeasurementsState, result: MeasurementsResult): MeasurementsState = when (result) {
        MeasurementsResult.Loading -> state.copy(isLoading = true, errorCode = null)

        is MeasurementsResult.Loaded -> state.copy(
            measurements = result.measurements,
            isLoading = false,
            errorCode = null,
        )

        is MeasurementsResult.LoadFailed -> state.copy(isLoading = false, errorCode = result.errorCode)
    }
}
