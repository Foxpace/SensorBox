package com.motionapps.wearoslib.protocol

import com.motionapps.sensorbox.core.error.AppErrorCode

sealed interface WearCommand {
    data object LaunchPhone : WearCommand

    data object SyncMeasurements : WearCommand

    data object RequestSensorList : WearCommand

    data class SensorList(val sensors: List<WearSensorInfo>) : WearCommand

    data class PrepareRecording(val sessionId: String, val request: WearRecordingRequest) : WearCommand

    data class CommitRecording(val sessionId: String, val startAtEpochMillis: Long) : WearCommand

    data class AbortRecording(val sessionId: String) : WearCommand

    data class StopRecording(val sessionId: String, val reason: WearStopReason) : WearCommand

    data class Acknowledgement(
        val sessionId: String,
        val command: WearSessionCommand,
        val outcome: WearAcknowledgementOutcome,
        val errorCode: AppErrorCode? = null,
        val failureCount: Int = 0,
    ) : WearCommand
}

data class WearRecordingRequest(
    val folderName: String,
    val sensorIds: List<Int>,
    val includesGps: Boolean,
    val durationMillis: Long = 0L,
    val measurementType: String = "ENDLESS",
)

enum class WearSessionCommand {
    PREPARE,
    COMMIT,
    ABORT,
    STOP,
}

enum class WearAcknowledgementOutcome {
    SUCCEEDED,
    REJECTED,
    FAILED,
}

enum class WearStopReason {
    USER_REQUEST,
    DURATION_EXPIRED,
    LOW_BATTERY,
    SOURCE_FAILURE,
    PAIRED_ABORT,
    SERVICE_DESTROYED,
}

data class WearSensorInfo(val type: Int, val name: String, val vendor: String, val isHeartRate: Boolean)
