package com.tomasrepcik.sensorbox.recording.session

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
