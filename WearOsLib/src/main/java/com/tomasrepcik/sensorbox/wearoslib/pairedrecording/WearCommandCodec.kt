package com.tomasrepcik.sensorbox.wearoslib.pairedrecording

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.failure.flatMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

object WearCommandCodec {
    const val PROTOCOL_VERSION = 7

    private val json = Json {
        classDiscriminator = "commandType"
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    fun encode(command: WearCommand): AppResult<ByteArray> = validate(command).flatMap {
        appResult(AppErrorCode.CONNECTIVITY, "Encode Wear command") {
            json.encodeToString(WearCommandEnvelope(PROTOCOL_VERSION, command)).encodeToByteArray()
        }
    }

    fun decode(payload: ByteArray): AppResult<WearCommand> = appResult(
        AppErrorCode.CONNECTIVITY,
        "Decode Wear protocol v$PROTOCOL_VERSION command",
    ) {
        val envelope = json.decodeFromString<WearCommandEnvelope>(payload.decodeToString())
        require(envelope.protocolVersion == PROTOCOL_VERSION) { "Unsupported Wear protocol version" }
        envelope.command
    }.flatMap { command -> validate(command).map { command } }

    private fun validate(command: WearCommand): AppResult<Unit> = if (command.isValid()) {
        AppResult.success(Unit)
    } else {
        AppResult.failure(AppError(AppErrorCode.VALIDATION, "Validate Wear protocol v$PROTOCOL_VERSION command"))
    }
}

@Serializable
private data class WearCommandEnvelope(val protocolVersion: Int, val command: WearCommand)

internal object AppErrorCodeNameSerializer : KSerializer<AppErrorCode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AppErrorCode", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AppErrorCode) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): AppErrorCode {
        val name = decoder.decodeString()
        return AppErrorCode.entries.firstOrNull { it.name == name }
            ?: error("Unknown application error code")
    }
}

private const val MAX_SENSORS = 64
private const val MAX_FOLDER_LENGTH = 100
private const val MAX_SENSOR_TEXT_LENGTH = 100
private const val MAX_SESSION_ID_LENGTH = 128
private const val MAX_ERROR_TEXT_LENGTH = 500
private const val MAX_ERROR_CONTEXT_ENTRIES = 16

private fun WearCommand.isValid(): Boolean = when (this) {
    WearCommand.LaunchPhone,
    WearCommand.RequestAvailableSensors,
    -> true

    is WearCommand.CheckWatchMeasurements -> validSessionId(requestId)

    is WearCommand.CopyWatchMeasurements -> validSessionId(requestId)

    is WearCommand.CancelWatchSync -> validSessionId(requestId)

    is WearCommand.WatchMeasurementsStatus -> isValid()

    is WearCommand.AvailableSensors -> sensors.size <= MAX_SENSORS && sensors.all(WearSensorInfo::isValid)

    is WearCommand.StartRecording -> isValid()

    is WearCommand.StopRecording -> validSessionId(sessionId)

    is WearCommand.RecordingResult -> isValid()
}

private fun WearSensorInfo.isValid(): Boolean = name.length <= MAX_SENSOR_TEXT_LENGTH &&
    vendor.length <= MAX_SENSOR_TEXT_LENGTH &&
    stringType.length <= MAX_SENSOR_TEXT_LENGTH &&
    maximumRange.isFinite() &&
    resolution.isFinite() &&
    power.isFinite()

private fun WearCommand.StartRecording.isValid(): Boolean = validSessionId(sessionId) &&
    request.folderName.isNotBlank() &&
    request.folderName.length <= MAX_FOLDER_LENGTH &&
    request.sensorIds.size <= MAX_SENSORS &&
    request.durationMillis >= 0L &&
    request.settings.isValid()

private fun WearCommand.RecordingResult.isValid(): Boolean = validSessionId(sessionId) &&
    failureCount >= 0 &&
    hasValidErrorContext() &&
    when (outcome) {
        WearRecordingOutcome.SUCCEEDED -> hasNoError()
        WearRecordingOutcome.FAILED -> hasValidError()
    }

private fun WearCommand.RecordingResult.hasValidErrorContext(): Boolean =
    errorContext.size <= MAX_ERROR_CONTEXT_ENTRIES && errorContext.all { (key, value) ->
        key.isNotBlank() && key.length <= MAX_ERROR_TEXT_LENGTH && value.length <= MAX_ERROR_TEXT_LENGTH
    }

private fun WearCommand.RecordingResult.hasNoError(): Boolean = errorCode == null &&
    errorOperation == null &&
    errorMessage == null &&
    errorContext.isEmpty() &&
    failureCount == 0

private fun WearCommand.RecordingResult.hasValidError(): Boolean = errorCode != null &&
    !errorOperation.isNullOrBlank() &&
    errorOperation.length <= MAX_ERROR_TEXT_LENGTH &&
    !errorMessage.isNullOrBlank() &&
    errorMessage.length <= MAX_ERROR_TEXT_LENGTH

private fun validSessionId(sessionId: String): Boolean = sessionId.isNotBlank() &&
    sessionId.length <= MAX_SESSION_ID_LENGTH &&
    sessionId.all { it.isLetterOrDigit() || it == '-' || it == '_' }

private fun WearCommand.WatchMeasurementsStatus.isValid(): Boolean = validSessionId(requestId) &&
    fileCount >= 0 && measurementCount >= 0 && measurementCount <= fileCount

private fun WearRecordingSettings.isValid(): Boolean =
    samplingPeriodIndex in 0..3 && gpsIntervalSeconds > 0 && gpsMinDistanceMeters >= 0
