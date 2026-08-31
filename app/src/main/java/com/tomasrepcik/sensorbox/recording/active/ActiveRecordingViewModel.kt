package com.tomasrepcik.sensorbox.recording.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingControlUseCase
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveRecordingViewModel @Inject constructor(
    private val recording: RecordingControlUseCase,
    private val sessionStore: RecordingSessionStore,
    private val elapsedRealtimeClock: ElapsedRealtimeClock,
    private val appFailures: AppFailureStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(RecordingState(session = sessionStore.state.value))
    private val mutableEffects = Channel<ActiveRecordingEffect>(Channel.BUFFERED)
    private var elapsedJob: Job? = null

    val state: StateFlow<RecordingState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        observeRecordingSession()
        observeRecordingStops()
    }

    fun accept(intent: ActiveRecordingIntent) {
        when (intent) {
            ActiveRecordingIntent.StopRecording -> stopRecording()
            is ActiveRecordingIntent.AddAnnotation -> recording.annotate(intent.text).showFailure()
        }
    }

    private fun observeRecordingSession() {
        viewModelScope.launch {
            sessionStore.state.collect(::onSessionChanged)
        }
    }

    private fun observeRecordingStops() {
        viewModelScope.launch {
            sessionStore.events.collect { event ->
                event.result.errorOrNull()?.let(appFailures::show)
            }
        }
    }

    private fun onSessionChanged(session: RecordingSessionState) {
        elapsedJob?.cancel()
        mutableState.value = RecordingSessionStateReducer.sessionChanged(state.value, session)
        if (session is RecordingSessionState.Running) startElapsedTicker(session.startedAtElapsedRealtime)
    }

    private fun startElapsedTicker(startedAt: Long) {
        elapsedJob = viewModelScope.launch {
            while (isActive) {
                val seconds = (elapsedRealtimeClock.nowMillis() - startedAt).coerceAtLeast(0) / 1_000L
                mutableState.value = state.value.copy(elapsedSeconds = seconds)
                delay(1_000L)
            }
        }
    }

    private fun stopRecording() {
        viewModelScope.launch {
            val result = recording.stop()
            if (result.isSuccess) {
                mutableEffects.send(ActiveRecordingEffect.RecordingStopped)
            } else {
                result.showFailure()
            }
        }
    }

    private fun AppResult<*>.showFailure() {
        if (isFailure) {
            appFailures.show(errorOrNull() ?: AppError(AppErrorCode.UNKNOWN, "Run active recording operation"))
        }
    }
}
