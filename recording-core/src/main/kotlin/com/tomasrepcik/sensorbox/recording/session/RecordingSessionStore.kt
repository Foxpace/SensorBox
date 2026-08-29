package com.tomasrepcik.sensorbox.recording.session

import com.tomasrepcik.sensorbox.core.error.AppResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
class RecordingSessionStore {
    private val mutableState = MutableStateFlow<RecordingSessionState>(RecordingSessionState.Idle)
    private val mutableEvents = MutableSharedFlow<RecordingSessionStopped>(extraBufferCapacity = EVENT_BUFFER_SIZE)
    val state: StateFlow<RecordingSessionState> = mutableState.asStateFlow()
    val events: SharedFlow<RecordingSessionStopped> = mutableEvents

    fun markRunning(state: RecordingSessionState.Running) {
        mutableState.value = state
    }

    fun markStopping() {
        mutableState.value = RecordingSessionState.Stopping
    }

    fun markIdle() {
        mutableState.value = RecordingSessionState.Idle
    }

    fun publishStopped(sessionId: String, reason: RecordingSessionStopReason, result: AppResult<Unit>) {
        mutableEvents.tryEmit(RecordingSessionStopped(sessionId, reason, result))
    }

    private companion object {
        const val EVENT_BUFFER_SIZE = 16
    }
}

enum class RecordingSessionStopReason {
    USER_REQUEST,
    DURATION_EXPIRED,
    LOW_BATTERY,
    SOURCE_FAILURE,
    SERVICE_DESTROYED,
}

data class RecordingSessionStopped(
    val sessionId: String,
    val reason: RecordingSessionStopReason,
    val result: AppResult<Unit>,
)
