package com.tomasrepcik.sensorbox.wearoslib.protocol

import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WearCommand {
    @Serializable
    data object LaunchPhone : WearCommand

    @Serializable
    data object SyncMeasurements : WearCommand

    @Serializable
    data object RequestAvailableSensors : WearCommand

    @Serializable
    data class AvailableSensors(val sensors: List<WearSensorInfo>) : WearCommand

    @Serializable
    data class StartRecording(val sessionId: String, val request: WearRecordingRequest) : WearCommand

    @Serializable
    data class StopRecording(val sessionId: String, val reason: WearStopReason) : WearCommand

    @Serializable
    data class RecordingResult(
        val sessionId: String,
        @SerialName("action")
        val operation: WearRecordingOperation,
        val outcome: WearRecordingOutcome,
        @Serializable(with = AppErrorCodeNameSerializer::class)
        val errorCode: AppErrorCode? = null,
        val errorOperation: String? = null,
        val errorMessage: String? = null,
        val errorContext: Map<String, String> = emptyMap(),
        val failureCount: Int = 0,
    ) : WearCommand
}

@Serializable
data class WearRecordingRequest(
    val folderName: String,
    val sensorIds: List<Int>,
    val includesGps: Boolean,
    val durationMillis: Long = 0L,
)

@Serializable
enum class WearRecordingOperation {
    START,
    STOP,
}

@Serializable
enum class WearRecordingOutcome {
    SUCCEEDED,
    FAILED,
}

@Serializable
enum class WearStopReason {
    USER_REQUEST,
    DURATION_EXPIRED,
    LOW_BATTERY,
    SOURCE_FAILURE,
    SERVICE_DESTROYED,
}

@Serializable
data class WearSensorInfo(
    val type: Int,
    val name: String,
    val vendor: String,
    val version: Int,
    val stringType: String,
    val maximumRange: Float,
    val resolution: Float,
    val power: Float,
    val minimumDelayMicros: Int,
    val maximumDelayMicros: Int,
    val reportingMode: Int,
    val isWakeUpSensor: Boolean,
)

fun WearCommand.recordingSessionId(): String? = when (this) {
    is WearCommand.StartRecording -> sessionId

    is WearCommand.StopRecording -> sessionId

    is WearCommand.RecordingResult -> sessionId

    WearCommand.LaunchPhone,
    WearCommand.RequestAvailableSensors,
    is WearCommand.AvailableSensors,
    WearCommand.SyncMeasurements,
    -> null
}
