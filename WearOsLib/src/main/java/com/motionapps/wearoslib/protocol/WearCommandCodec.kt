package com.motionapps.wearoslib.protocol

import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object WearCommandCodec {
    fun encode(command: WearCommand): Result<ByteArray> {
        val itemCount = when (command) {
            is WearCommand.SensorList -> command.sensors.size
            is WearCommand.StartMeasurement -> command.sensorIds.size
            else -> 0
        }
        if (itemCount > MAX_SENSORS) {
            return Result.failure(AppError(AppError.Kind.CONNECTIVITY, "Validate Wear command"))
        }
        return appResult(AppError.Kind.CONNECTIVITY, "Encode Wear command") {
            ByteArrayOutputStream().use { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.writeInt(MAGIC)
                    output.writeByte(VERSION)
                    output.writeCommand(command)
                }
                bytes.toByteArray()
            }
        }
    }

    fun decode(payload: ByteArray): Result<WearCommand> = appResult(AppError.Kind.CONNECTIVITY, "Read Wear command") {
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            val validHeader = input.readInt() == MAGIC && input.readUnsignedByte() == VERSION
            if (validHeader) {
                input.readCommand()
            } else {
                Result.failure(
                    AppError(AppError.Kind.CONNECTIVITY, "Validate Wear command header"),
                )
            }
        }
    }.flatMap { it }

    private fun DataOutputStream.writeCommand(command: WearCommand) {
        when (command) {
            WearCommand.LaunchPhone -> writeByte(TYPE_LAUNCH_PHONE)
            is WearCommand.StartMeasurement -> writeMeasurement(command)
            WearCommand.StopMeasurement -> writeByte(TYPE_STOP_MEASUREMENT)
            WearCommand.SyncMeasurements -> writeByte(TYPE_SYNC_MEASUREMENTS)
            WearCommand.RequestSensorList -> writeByte(TYPE_REQUEST_SENSOR_LIST)
            is WearCommand.SensorList -> writeSensorList(command)
        }
    }

    private fun DataOutputStream.writeSensorList(command: WearCommand.SensorList) {
        writeByte(TYPE_SENSOR_LIST)
        writeByte(command.sensors.size)
        command.sensors.forEach { sensor ->
            writeInt(sensor.type)
            writeUTF(sensor.name.take(MAX_SENSOR_TEXT_LENGTH))
            writeUTF(sensor.vendor.take(MAX_SENSOR_TEXT_LENGTH))
            writeBoolean(sensor.isHeartRate)
        }
    }

    private fun DataOutputStream.writeMeasurement(command: WearCommand.StartMeasurement) {
        writeByte(TYPE_START_MEASUREMENT)
        writeUTF(command.folderName.take(MAX_FOLDER_LENGTH))
        writeBoolean(command.includesGps)
        writeByte(command.sensorIds.size)
        command.sensorIds.forEach(::writeInt)
        writeLong(command.startAtEpochMillis)
        writeLong(command.durationMillis)
        writeUTF(command.measurementType.take(MAX_TYPE_LENGTH))
    }

    private fun DataInputStream.readCommand(): Result<WearCommand> = when (readUnsignedByte()) {
        TYPE_LAUNCH_PHONE -> Result.success(WearCommand.LaunchPhone)
        TYPE_START_MEASUREMENT -> readMeasurement()
        TYPE_STOP_MEASUREMENT -> Result.success(WearCommand.StopMeasurement)
        TYPE_SYNC_MEASUREMENTS -> Result.success(WearCommand.SyncMeasurements)
        TYPE_REQUEST_SENSOR_LIST -> Result.success(WearCommand.RequestSensorList)
        TYPE_SENSOR_LIST -> readSensorList()
        else -> Result.failure(AppError(AppError.Kind.CONNECTIVITY, "Validate Wear command type"))
    }

    private fun DataInputStream.readSensorList(): Result<WearCommand.SensorList> {
        val count = readUnsignedByte()
        if (count > MAX_SENSORS) {
            return Result.failure(AppError(AppError.Kind.CONNECTIVITY, "Validate Wear sensor count"))
        }
        return Result.success(
            WearCommand.SensorList(
                List(count) { WearSensorInfo(readInt(), readUTF(), readUTF(), readBoolean()) },
            ),
        )
    }

    private fun DataInputStream.readMeasurement(): Result<WearCommand.StartMeasurement> {
        val folderName = readUTF()
        val includesGps = readBoolean()
        val sensorCount = readUnsignedByte()
        if (sensorCount > MAX_SENSORS) {
            return Result.failure(AppError(AppError.Kind.CONNECTIVITY, "Validate Wear sensor count"))
        }
        val sensorIds = List(sensorCount) { readInt() }
        val startAt = if (available() >= Long.SIZE_BYTES) readLong() else System.currentTimeMillis()
        val duration = if (available() >= Long.SIZE_BYTES) readLong() else 0L
        val type = if (available() > 0) readUTF() else "ENDLESS"
        return Result.success(
            WearCommand.StartMeasurement(
                folderName = folderName,
                sensorIds = sensorIds,
                includesGps = includesGps,
                startAtEpochMillis = startAt,
                durationMillis = duration,
                measurementType = type,
            ),
        )
    }

    private const val MAGIC = 0x53425831
    private const val VERSION = 1
    private const val TYPE_LAUNCH_PHONE = 1
    private const val TYPE_START_MEASUREMENT = 2
    private const val TYPE_STOP_MEASUREMENT = 3
    private const val TYPE_SYNC_MEASUREMENTS = 4
    private const val TYPE_REQUEST_SENSOR_LIST = 5
    private const val TYPE_SENSOR_LIST = 6
    private const val MAX_SENSORS = 64
    private const val MAX_FOLDER_LENGTH = 100
    private const val MAX_TYPE_LENGTH = 32
    private const val MAX_SENSOR_TEXT_LENGTH = 100
}
