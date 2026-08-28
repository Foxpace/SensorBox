package com.tomasrepcik.sensorbox.sensorservices.session

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
    private val mutableEvents = MutableSharedFlow<MeasurementStopped>(extraBufferCapacity = EVENT_BUFFER_SIZE)
    val state: StateFlow<MeasurementSessionState> = mutableState.asStateFlow()
    val events: SharedFlow<MeasurementStopped> = mutableEvents

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
        mutableEvents.tryEmit(MeasurementStopped(sessionId, reason, result))
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

data class MeasurementStopped(val sessionId: String, val reason: MeasurementStopReason, val result: AppResult<Unit>)
