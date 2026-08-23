package com.motionapps.sensorservices.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementSessionStore @Inject constructor() {
    private val mutableState = MutableStateFlow<MeasurementSessionState>(MeasurementSessionState.Idle)
    val state: StateFlow<MeasurementSessionState> = mutableState.asStateFlow()

    fun markRunning(state: MeasurementSessionState.Running) {
        mutableState.value = state
    }

    fun markStopping() {
        mutableState.value = MeasurementSessionState.Stopping
    }

    fun markIdle() {
        mutableState.value = MeasurementSessionState.Idle
    }
}
