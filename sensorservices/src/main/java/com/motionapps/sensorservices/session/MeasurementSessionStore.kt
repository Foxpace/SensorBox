package com.motionapps.sensorservices.session

import com.tomasrepcik.sensorbox.core.error.AppResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementSessionStore @Inject constructor() {
    private val mutableState = MutableStateFlow<MeasurementSessionState>(MeasurementSessionState.Idle)
    private val mutableEvents = MutableSharedFlow<MeasurementSessionEvent>(extraBufferCapacity = EVENT_BUFFER_SIZE)
    val state: StateFlow<MeasurementSessionState> = mutableState.asStateFlow()
    val events: SharedFlow<MeasurementSessionEvent> = mutableEvents

    fun markRunning(state: MeasurementSessionState.Running) {
        mutableState.value = state
    }

    fun markStopping() {
        mutableState.value = MeasurementSessionState.Stopping
    }

    fun markIdle() {
        mutableState.value = MeasurementSessionState.Idle
    }

    fun publishStopped(sessionId: String, reason: MeasurementStopReason, result: AppResult<Unit>) {
        mutableEvents.tryEmit(MeasurementSessionEvent.Stopped(sessionId, reason, result))
    }

    private companion object {
        const val EVENT_BUFFER_SIZE = 16
    }
}

enum class MeasurementStopReason {
    USER_REQUEST,
    DURATION_EXPIRED,
    LOW_BATTERY,
    SOURCE_FAILURE,
    SERVICE_DESTROYED,
}

sealed interface MeasurementSessionEvent {
    data class Stopped(val sessionId: String, val reason: MeasurementStopReason, val result: AppResult<Unit>) :
        MeasurementSessionEvent
}
