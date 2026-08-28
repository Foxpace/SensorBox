package com.tomasrepcik.sensorbox.sensorservices.session

sealed interface RecordingSessionState {
    data object Idle : RecordingSessionState

    data class Running(
        val sessionId: String,
        val folderName: String,
        val startedAtElapsedRealtime: Long,
        val sensorIds: List<Int>,
        val includesGps: Boolean,
    ) : RecordingSessionState

    data object Stopping : RecordingSessionState
}
