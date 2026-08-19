package com.motionapps.sensorservices.session

sealed interface MeasurementSessionState {
    data object Idle : MeasurementSessionState

    data class Running(
        val sessionId: String,
        val folderName: String,
        val startedAtElapsedRealtime: Long,
        val sensorIds: List<Int>,
        val includesGps: Boolean,
    ) : MeasurementSessionState

    data object Stopping : MeasurementSessionState
}
