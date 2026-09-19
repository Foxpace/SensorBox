package com.tomasrepcik.sensorbox.wearoslib.pairedrecording

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import kotlinx.serialization.Serializable

@Serializable
sealed interface WearCommand {
    @Serializable
    data object LaunchPhone : WearCommand

    @Serializable
    data class CheckWatchMeasurements(val requestId: String) : WearCommand

    @Serializable
    data class CopyWatchMeasurements(val requestId: String) : WearCommand

    @Serializable
    data class CancelWatchSync(val requestId: String) : WearCommand

    @Serializable
    data class WatchMeasurementsStatus(
        val requestId: String,
        val fileCount: Int,
        val measurementCount: Int,
        val finished: Boolean = false,
        val failed: Boolean = false,
    ) : WearCommand

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
    val settings: WearRecordingSettings,
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

    is WearCommand.CheckWatchMeasurements,
    is WearCommand.CopyWatchMeasurements,
    is WearCommand.CancelWatchSync,
    is WearCommand.WatchMeasurementsStatus,
    WearCommand.LaunchPhone,
    WearCommand.RequestAvailableSensors,
    is WearCommand.AvailableSensors,
    -> null
}

@Serializable
data class WearRecordingSettings(
    val samplingPeriodIndex: Int,
    val stopOnLowBattery: Boolean,
    val useWakeLock: Boolean,
    val gpsIntervalSeconds: Int,
    val gpsMinDistanceMeters: Int,
)
