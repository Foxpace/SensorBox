package com.tomasrepcik.sensorbox.measurements.list

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
class MeasurementsViewModel @Inject constructor(
    private val repository: MeasurementRepository,
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MeasurementsState())
    private val mutableEffects = Channel<MeasurementsEffect>(Channel.BUFFERED)

    val state = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    fun accept(intent: MeasurementsIntent) {
        when (intent) {
            MeasurementsIntent.Refresh -> refresh()

            is MeasurementsIntent.OpenDetails -> mutableEffects.trySend(
                MeasurementsEffect.OpenDetails(intent.measurementId),
            )
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            mutableState.value = MeasurementsReducer.reduce(state.value, MeasurementsResult.Loading)
            repository.loadMeasurements().fold(
                onSuccess = { measurements ->
                    mutableState.value = MeasurementsReducer.reduce(
                        state.value,
                        MeasurementsResult.Loaded(measurements),
                    )
                },
                onFailure = { error ->
                    mutableState.value = MeasurementsReducer.reduce(
                        state.value,
                        MeasurementsResult.LoadFailed(error.code),
                    )
                    appFailures.show(error)
                },
            )
        }
    }
}
