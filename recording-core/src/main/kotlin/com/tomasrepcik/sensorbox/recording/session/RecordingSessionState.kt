package com.tomasrepcik.sensorbox.recording.session

sealed interface RecordingSessionState {
    data object Idle : RecordingSessionState

    data class Running(
        val sessionId: String,
        val folderName: String,
        val startedAtElapsedRealtime: Long,
        val sensorIds: List<Int>,
        val includesGps: Boolean,
        val durationMillis: Long = 0L,
        val sensorSamplingPeriod: Int = 0,
    ) : RecordingSessionState

    data object Stopping : RecordingSessionState
}
