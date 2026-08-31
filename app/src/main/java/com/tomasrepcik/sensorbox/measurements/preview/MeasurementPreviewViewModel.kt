package com.tomasrepcik.sensorbox.measurements.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementDetails
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileContent
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementPreviewViewModel @Inject constructor(
    private val repository: MeasurementRepository,
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MeasurementPreviewState())
    private var activeMeasurementId: String? = null
    private var activeFileId: String? = null
    private var loadSequence = 0L
    private var loadJob: Job? = null

    val state = mutableState.asStateFlow()

    fun accept(intent: MeasurementPreviewIntent) {
        when (intent) {
            is MeasurementPreviewIntent.Load -> load(intent.measurementId, intent.fileId)

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
        val sameFile = activeMeasurementId == measurementId && activeFileId == fileId
        if (sameFile && (state.value.isLoading || state.value.content != null)) return

        activeMeasurementId = measurementId
        activeFileId = fileId
        loadSequence += 1
        val sequence = loadSequence
        loadJob?.cancel()
        mutableState.value = MeasurementPreviewReducer.reduce(
            state.value,
            MeasurementPreviewResult.Loading,
        )
        loadJob = viewModelScope.launch { loadFromArchive(measurementId, fileId, sequence) }
    }

    private suspend fun loadFromArchive(measurementId: String, fileId: String, sequence: Long) {
        when (val detailsResult = repository.loadMeasurementDetails(measurementId)) {
            is AppResult.Success -> loadFile(detailsResult.value, measurementId, fileId, sequence)
            is AppResult.Failure -> failLoad(detailsResult.error, sequence)
        }
    }

    private suspend fun loadFile(details: MeasurementDetails, measurementId: String, fileId: String, sequence: Long) {
        val file = details.files.firstOrNull { it.id == fileId }
        if (file == null) {
            failLoad(AppError(AppErrorCode.STORAGE, "Find measurement file"), sequence)
            return
        }
        if (sequence != loadSequence) return
        mutableState.value = MeasurementPreviewReducer.reduce(
            state.value,
            MeasurementPreviewResult.FileFound(
                file = file,
                sensorMetadata = details.sensorMetadataByFile[file.id].orEmpty(),
            ),
        )
        val contentResult = repository.loadMeasurementFile(measurementId, fileId) { progress ->
            reportProgress(sequence, progress)
        }
        when (contentResult) {
            is AppResult.Success -> completeLoad(contentResult.value, sequence)
            is AppResult.Failure -> failLoad(contentResult.error, sequence)
        }
    }

    private fun reportProgress(sequence: Long, progress: Float) {
        mutableState.update { current ->
            if (sequence == loadSequence) {
                MeasurementPreviewReducer.reduce(current, MeasurementPreviewResult.Progress(progress))
            } else {
                current
            }
        }
    }

    private fun completeLoad(content: MeasurementFileContent, sequence: Long) {
        if (sequence != loadSequence) return
        mutableState.value = MeasurementPreviewReducer.reduce(
            state.value,
            MeasurementPreviewResult.Loaded(content),
        )
    }

    private fun failLoad(error: AppError, sequence: Long) {
        if (sequence != loadSequence) return
        mutableState.value = MeasurementPreviewReducer.reduce(
            state.value,
            MeasurementPreviewResult.LoadFailed(error.code),
        )
        appFailures.show(error)
    }
}
