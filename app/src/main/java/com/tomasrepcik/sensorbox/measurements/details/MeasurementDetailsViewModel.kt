package com.tomasrepcik.sensorbox.measurements.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeasurementDetailsViewModel @Inject constructor(
    private val repository: MeasurementRepository,
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MeasurementDetailsState())
    private val mutableEffects = Channel<MeasurementDetailsEffect>(Channel.BUFFERED)

    val state = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    fun accept(intent: MeasurementDetailsIntent) {
        when (intent) {
            is MeasurementDetailsIntent.Load -> load(intent.measurementId)
            is MeasurementDetailsIntent.OpenFile -> openFile(intent.fileId)
        }
    }

    private fun load(measurementId: String) {
        viewModelScope.launch {
            mutableState.value = MeasurementDetailsReducer.reduce(MeasurementDetailsResult.Loading)
            repository.loadMeasurementDetails(measurementId).fold(
                onSuccess = { details ->
                    mutableState.value = MeasurementDetailsReducer.reduce(MeasurementDetailsResult.Loaded(details))
                },
                onFailure = { error ->
                    mutableState.value = MeasurementDetailsReducer.reduce(
                        MeasurementDetailsResult.LoadFailed(error.code),
                    )
                    appFailures.show(error)
                },
            )
        }
    }

    private fun openFile(fileId: String) {
        val details = state.value.details ?: return
        val file = details.files.firstOrNull { it.id == fileId } ?: return
        mutableEffects.trySend(
            MeasurementDetailsEffect.OpenFile(
                measurementId = details.summary.id,
                fileId = file.id,
            ),
        )
    }
}
