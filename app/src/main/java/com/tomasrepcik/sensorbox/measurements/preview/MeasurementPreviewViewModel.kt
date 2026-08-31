package com.tomasrepcik.sensorbox.measurements.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementDetails
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementPreviewViewModel @Inject constructor(
    private val repository: MeasurementRepository,
    private val appFailures: AppFailureStore,
    private val previewStore: MeasurementPreviewStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MeasurementPreviewState())

    val state = mutableState.asStateFlow()

    fun accept(intent: MeasurementPreviewIntent) {
        when (intent) {
            is MeasurementPreviewIntent.Load -> load(intent.measurementId, intent.fileId)

            is MeasurementPreviewIntent.Show -> {
                mutableState.value = MeasurementPreviewReducer.show(state.value, intent)
            }

            is MeasurementPreviewIntent.ZoomSensorChartTimeWindow -> {
                mutableState.value = MeasurementPreviewReducer.zoomSensorChartTimeWindow(
                    state.value,
                    intent.zoomFactor,
                    intent.focalPointFraction,
                )
            }

            is MeasurementPreviewIntent.ShiftSensorChartTimeWindow -> {
                mutableState.value = MeasurementPreviewReducer.shiftSensorChartTimeWindow(
                    state.value,
                    intent.visibleWindowFraction,
                )
            }

            MeasurementPreviewIntent.ShowEntireSensorChartTimeRange -> {
                mutableState.value = MeasurementPreviewReducer.showEntireSensorChartTimeRange(state.value)
            }
        }
    }

    private fun load(measurementId: String, fileId: String) {
        if (state.value.file?.id == fileId && state.value.content != null) return
        val cachedPreview = previewStore.take(measurementId, fileId)
        if (cachedPreview != null) {
            show(cachedPreview)
            return
        }
        mutableState.value = MeasurementPreviewReducer.startLoading(state.value)
        viewModelScope.launch { loadFromArchive(measurementId, fileId) }
    }

    private suspend fun loadFromArchive(measurementId: String, fileId: String) {
        when (val detailsResult = repository.loadMeasurementDetails(measurementId)) {
            is AppResult.Success -> loadFile(detailsResult.value, measurementId, fileId)
            is AppResult.Failure -> failLoad(detailsResult.error)
        }
    }

    private suspend fun loadFile(details: MeasurementDetails, measurementId: String, fileId: String) {
        val file = details.files.firstOrNull { it.id == fileId }
        if (file == null) {
            failLoad(AppError(AppErrorCode.STORAGE, "Find measurement file"))
            return
        }
        when (val contentResult = repository.loadMeasurementFile(measurementId, fileId) {}) {
            is AppResult.Success -> show(
                MeasurementPreviewData(
                    file = file,
                    content = contentResult.value,
                    sensorMetadata = details.sensorMetadataByFile[file.id].orEmpty(),
                ),
            )

            is AppResult.Failure -> failLoad(contentResult.error)
        }
    }

    private fun show(preview: MeasurementPreviewData) {
        mutableState.value = MeasurementPreviewReducer.show(
            state.value,
            MeasurementPreviewIntent.Show(preview.file, preview.content, preview.sensorMetadata),
        )
    }

    private fun failLoad(error: AppError) {
        mutableState.value = MeasurementPreviewReducer.failLoading(state.value, error.code)
        appFailures.show(error)
    }
}
