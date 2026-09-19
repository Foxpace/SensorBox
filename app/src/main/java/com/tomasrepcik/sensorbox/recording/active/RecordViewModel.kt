package com.tomasrepcik.sensorbox.recording.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.sources.AvailableRecordingSourcesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val availableSources: AvailableRecordingSourcesUseCase,
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initialState())
    private val mutableEffects = Channel<RecordEffect>(Channel.BUFFERED)

    val state: StateFlow<RecordingState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        observeAvailableSources()
    }

    fun accept(intent: RecordIntent) {
        val next = RecordReducer.reduce(state.value, intent)
        mutableState.value = next.state
        next.effect?.let(mutableEffects::trySend)
    }

    private fun initialState() = RecordingState(
        sensors = availableSources.current.phoneSensors,
        watchSensors = availableSources.current.watchSensors,
        isWatchConnected = availableSources.current.isWatchConnected,
    )

    private fun observeAvailableSources() {
        viewModelScope.launch {
            availableSources.observe().catch { cause ->
                appFailures.show(
                    AppError(
                        AppErrorCode.UNKNOWN,
                        "Observe available recording sources",
                        "Recording source observation failed",
                        cause,
                    ),
                )
            }.collect { sources ->
                mutableState.value = state.value.copy(
                    sensors = sources.phoneSensors,
                    isWatchConnected = sources.isWatchConnected,
                    watchSensors = sources.watchSensors,
                )
            }
        }
    }
}
