package com.motionapps.wearoslib.protocol

import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object WearCommandCodec {
    const val PROTOCOL_VERSION = 3

    fun encode(command: WearCommand): AppResult<ByteArray> = validate(command).flatMap {
        appResult(AppErrorCode.CONNECTIVITY, "Encode Wear command") {
            ByteArrayOutputStream().use { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.writeInt(MAGIC)
                    output.writeByte(PROTOCOL_VERSION)
                    output.writeCommand(command)
                }
                bytes.toByteArray()
            }
        }
    }

    fun decode(payload: ByteArray): AppResult<WearCommand> = appResult(
        AppErrorCode.CONNECTIVITY,
        "Decode Wear protocol v3 command",
    ) {
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            require(input.readInt() == MAGIC) { "Unsupported Wear protocol magic" }
            require(input.readUnsignedByte() == PROTOCOL_VERSION) { "Unsupported Wear protocol version" }
            input.readCommand().also { require(input.available() == 0) { "Trailing Wear command bytes" } }
        }
    }.flatMap { command -> validate(command).map { command } }

    private fun validate(command: WearCommand): AppResult<Unit> = if (command.isValid()) {
        AppResult.success(Unit)
    } else {
        AppResult.failure(AppError(AppErrorCode.VALIDATION, "Validate Wear protocol v3 command"))
    }

    private fun DataOutputStream.writeCommand(command: WearCommand) {
        when (command) {
            WearCommand.LaunchPhone -> writeByte(TYPE_LAUNCH_PHONE)

            WearCommand.SyncMeasurements -> writeByte(TYPE_SYNC_MEASUREMENTS)

            WearCommand.RequestSensorList -> writeByte(TYPE_REQUEST_SENSOR_LIST)

            is WearCommand.SensorList -> writeSensorList(command)

            is WearCommand.PrepareRecording -> writePrepare(command)

            is WearCommand.CommitRecording -> {
                writeByte(TYPE_COMMIT_RECORDING)
                writeUTF(command.sessionId)
                writeLong(command.startAtEpochMillis)
            }

            is WearCommand.AbortRecording -> {
                writeByte(TYPE_ABORT_RECORDING)
                writeUTF(command.sessionId)
            }

            is WearCommand.StopRecording -> {
                writeByte(TYPE_STOP_RECORDING)
                writeUTF(command.sessionId)
                writeUTF(command.reason.name)
            }

            is WearCommand.Acknowledgement -> writeAcknowledgement(command)
        }
    }

    private fun DataOutputStream.writePrepare(command: WearCommand.PrepareRecording) {
        writeByte(TYPE_PREPARE_RECORDING)
        writeUTF(command.sessionId)
        writeUTF(command.request.folderName)
        writeBoolean(command.request.includesGps)
        writeByte(command.request.sensorIds.size)
        command.request.sensorIds.forEach(::writeInt)
        writeLong(command.request.durationMillis)
    }

    private fun DataOutputStream.writeAcknowledgement(command: WearCommand.Acknowledgement) {
        writeByte(TYPE_ACKNOWLEDGEMENT)
        writeUTF(command.sessionId)
        writeUTF(command.command.name)
        writeUTF(command.outcome.name)
        writeBoolean(command.errorCode != null)
        command.errorCode?.let { writeUTF(it.name) }
        writeInt(command.failureCount)
    }

    private fun DataOutputStream.writeSensorList(command: WearCommand.SensorList) {
        writeByte(TYPE_SENSOR_LIST)
        writeByte(command.sensors.size)
        command.sensors.forEach { sensor ->
            writeInt(sensor.type)
            writeUTF(sensor.name)
            writeUTF(sensor.vendor)
        }
    }

    private fun DataInputStream.readCommand(): WearCommand = when (readUnsignedByte()) {
        TYPE_LAUNCH_PHONE -> WearCommand.LaunchPhone
        TYPE_SYNC_MEASUREMENTS -> WearCommand.SyncMeasurements
        TYPE_REQUEST_SENSOR_LIST -> WearCommand.RequestSensorList
        TYPE_SENSOR_LIST -> readSensorList()
        TYPE_PREPARE_RECORDING -> readPrepare()
        TYPE_COMMIT_RECORDING -> WearCommand.CommitRecording(readUTF(), readLong())
        TYPE_ABORT_RECORDING -> WearCommand.AbortRecording(readUTF())
        TYPE_STOP_RECORDING -> WearCommand.StopRecording(readUTF(), readNamedEnum(WearStopReason.entries))
        TYPE_ACKNOWLEDGEMENT -> readAcknowledgement()
        else -> error("Unknown Wear command type")
    }

    private fun DataInputStream.readPrepare(): WearCommand.PrepareRecording {
        val sessionId = readUTF()
        val folderName = readUTF()
        val includesGps = readBoolean()
        val sensorCount = readUnsignedByte()
        require(sensorCount <= MAX_SENSORS) { "Too many Wear sensors" }
        val sensorIds = List(sensorCount) { readInt() }
        return WearCommand.PrepareRecording(
            sessionId = sessionId,
            request = WearRecordingRequest(
                folderName = folderName,
                sensorIds = sensorIds,
                includesGps = includesGps,
                durationMillis = readLong(),
            ),
        )
    }

    private fun DataInputStream.readAcknowledgement(): WearCommand.Acknowledgement {
        val sessionId = readUTF()
        val command = readNamedEnum(WearSessionCommand.entries)
        val outcome = readNamedEnum(WearAcknowledgementOutcome.entries)
        val errorCode = if (readBoolean()) readNamedEnum(AppErrorCode.entries) else null
        return WearCommand.Acknowledgement(sessionId, command, outcome, errorCode, readInt())
    }

    private fun DataInputStream.readSensorList(): WearCommand.SensorList {
        val count = readUnsignedByte()
        require(count <= MAX_SENSORS) { "Too many Wear sensors" }
        return WearCommand.SensorList(
            List(count) { WearSensorInfo(readInt(), readUTF(), readUTF()) },
        )
    }

    private const val MAGIC = 0x53425833
    private const val TYPE_LAUNCH_PHONE = 1
    private const val TYPE_SYNC_MEASUREMENTS = 2
    private const val TYPE_REQUEST_SENSOR_LIST = 3
    private const val TYPE_SENSOR_LIST = 4
    private const val TYPE_PREPARE_RECORDING = 10
    private const val TYPE_COMMIT_RECORDING = 11
    private const val TYPE_ABORT_RECORDING = 12
    private const val TYPE_STOP_RECORDING = 13
    private const val TYPE_ACKNOWLEDGEMENT = 14
}

private const val MAX_SENSORS = 64
private const val MAX_FOLDER_LENGTH = 100
private const val MAX_SENSOR_TEXT_LENGTH = 100
private const val MAX_SESSION_ID_LENGTH = 128

private fun WearCommand.isValid(): Boolean = when (this) {
    WearCommand.LaunchPhone,
    WearCommand.RequestSensorList,
    WearCommand.SyncMeasurements,
    -> true

    is WearCommand.SensorList -> sensors.size <= MAX_SENSORS && sensors.all(WearSensorInfo::isValid)

    is WearCommand.PrepareRecording -> isValid()

    is WearCommand.CommitRecording -> validSessionId(sessionId) && startAtEpochMillis >= 0L

    is WearCommand.AbortRecording -> validSessionId(sessionId)

    is WearCommand.StopRecording -> validSessionId(sessionId)

    is WearCommand.Acknowledgement -> isValid()
}

private fun WearSensorInfo.isValid(): Boolean =
    name.length <= MAX_SENSOR_TEXT_LENGTH && vendor.length <= MAX_SENSOR_TEXT_LENGTH

private fun WearCommand.PrepareRecording.isValid(): Boolean = validSessionId(sessionId) &&
    request.folderName.isNotBlank() &&
    request.folderName.length <= MAX_FOLDER_LENGTH &&
    request.sensorIds.size <= MAX_SENSORS &&
    request.durationMillis >= 0L

private fun WearCommand.Acknowledgement.isValid(): Boolean = validSessionId(sessionId) &&
    failureCount >= 0 &&
    when (outcome) {
        WearAcknowledgementOutcome.SUCCEEDED -> errorCode == null && failureCount == 0

        WearAcknowledgementOutcome.REJECTED,
        WearAcknowledgementOutcome.FAILED,
        -> errorCode != null
    }

private fun validSessionId(sessionId: String): Boolean = sessionId.isNotBlank() &&
    sessionId.length <= MAX_SESSION_ID_LENGTH &&
    sessionId.all { it.isLetterOrDigit() || it == '-' || it == '_' }

private inline fun <reified T : Enum<T>> DataInputStream.readNamedEnum(values: List<T>): T {
    val name = readUTF()
    return values.firstOrNull { it.name == name } ?: error("Unknown Wear protocol enum value")
}
