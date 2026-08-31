package com.tomasrepcik.sensorbox.measurements.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.measurements.preview.MeasurementPreviewData
import com.tomasrepcik.sensorbox.measurements.preview.MeasurementPreviewStore
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileContent
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementLoadingViewModel @Inject constructor(
    private val repository: MeasurementRepository,
    private val appFailures: AppFailureStore,
    private val previewStore: MeasurementPreviewStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MeasurementLoadingState())
    private val mutableEffects = Channel<MeasurementLoadingEffect>(Channel.BUFFERED)
    private var loadSequence = 0L
    private var loadJob: Job? = null

    val state = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    fun accept(intent: MeasurementLoadingIntent) {
        when (intent) {
            is MeasurementLoadingIntent.Load -> load(intent.measurementId, intent.fileId)
            MeasurementLoadingIntent.Cancel -> cancel()
        }
    }

    private fun load(measurementId: String, fileId: String) {
        loadSequence += 1
        val sequence = loadSequence
        mutableState.value = MeasurementLoadingReducer.reduce(
            state.value,
            MeasurementLoadingResult.Starting,
        )
        loadJob?.cancel()
        loadJob = viewModelScope.launch { resolveRequestAndLoad(measurementId, fileId, sequence) }
    }

    private fun cancel() {
        loadSequence += 1
        loadJob?.cancel()
        mutableState.value = MeasurementLoadingReducer.reduce(
            state.value,
            MeasurementLoadingResult.Cancelled,
        )
    }

    private suspend fun resolveRequestAndLoad(measurementId: String, fileId: String, sequence: Long) {
        when (val result = repository.loadMeasurementDetails(measurementId)) {
            is AppResult.Success -> {
                val details = result.value
                val file = details.files.firstOrNull { it.id == fileId }
                if (file == null) {
                    failLoad(AppError(AppErrorCode.STORAGE, "Find measurement file"), sequence)
                    return
                }
                val request = MeasurementLoadingRequest(
                    measurementId = measurementId,
                    file = file,
                    sensorMetadata = details.sensorMetadataByFile[file.id].orEmpty(),
                )
                mutableState.value = MeasurementLoadingReducer.reduce(
                    state.value,
                    MeasurementLoadingResult.Loading(request),
                )
                loadFile(request, sequence)
            }

            is AppResult.Failure -> failLoad(result.error, sequence)
        }
    }

    private suspend fun loadFile(request: MeasurementLoadingRequest, sequence: Long) {
        repository.loadMeasurementFile(request.measurementId, request.file.id) { progress ->
            reportProgress(sequence, progress)
        }.fold(
            onSuccess = { content -> completeLoad(request, content, sequence) },
            onFailure = { error -> failLoad(error, sequence) },
        )
    }

    private fun reportProgress(sequence: Long, progress: Float) {
        mutableState.update { current ->
            if (sequence == loadSequence) {
                MeasurementLoadingReducer.reduce(current, MeasurementLoadingResult.Progress(progress))
            } else {
                current
            }
        }
    }

    private fun completeLoad(request: MeasurementLoadingRequest, content: MeasurementFileContent, sequence: Long) {
        if (sequence != loadSequence) return
        mutableState.value = MeasurementLoadingReducer.reduce(state.value, MeasurementLoadingResult.Loaded)
        previewStore.put(
            measurementId = request.measurementId,
            fileId = request.file.id,
            data = MeasurementPreviewData(request.file, content, request.sensorMetadata),
        )
        mutableEffects.trySend(
            MeasurementLoadingEffect.OpenPreview(
                request.measurementId,
                request.file.id,
            ),
        )
    }

    private fun failLoad(error: AppError, sequence: Long) {
        if (sequence != loadSequence) return
        mutableState.value = MeasurementLoadingReducer.reduce(
            state.value,
            MeasurementLoadingResult.LoadFailed(error.code),
        )
        appFailures.show(error)
    }
}
