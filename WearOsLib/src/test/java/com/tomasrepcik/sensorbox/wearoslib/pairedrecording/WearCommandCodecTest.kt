package com.tomasrepcik.sensorbox.wearoslib.pairedrecording

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearCommandCodecTest {
    @Test
    fun `Given current commands When round tripped Then every field survives`() {
        val request = WearRecordingRequest(
            folderName = "shared_session",
            sensorIds = listOf(1, 4, 21),
            includesGps = true,
            durationMillis = 45_000L,
            settings = WearRecordingSettings(0,
                stopOnLowBattery = true,
                useWakeLock = false,
                gpsIntervalSeconds = 1,
                gpsMinDistanceMeters = 0
            ),
        )
        val commands = listOf(
            WearCommand.LaunchPhone,
            WearCommand.CheckWatchMeasurements("check"),
            WearCommand.CopyWatchMeasurements("copy"),
            WearCommand.CancelWatchSync("copy"),
            WearCommand.WatchMeasurementsStatus("copy", 3, 1, finished = true),
            WearCommand.RequestAvailableSensors,
            WearCommand.AvailableSensors(
                listOf(
                    WearSensorInfo(
                        type = 1,
                        name = "Accelerometer",
                        vendor = "Fixture",
                        version = 7,
                        stringType = "android.sensor.accelerometer",
                        maximumRange = 78.4f,
                        resolution = 0.0024f,
                        power = 0.25f,
                        minimumDelayMicros = 5_000,
                        maximumDelayMicros = 200_000,
                        reportingMode = 0,
                        isWakeUpSensor = true,
                    ),
                ),
            ),
            WearCommand.StartRecording("session-123", request),
            WearCommand.StopRecording("session-123", WearStopReason.LOW_BATTERY),
            WearCommand.RecordingResult(
                sessionId = "session-123",
                operation = WearRecordingOperation.START,
                outcome = WearRecordingOutcome.SUCCEEDED,
            ),
            WearCommand.RecordingResult(
                sessionId = "session-123",
                operation = WearRecordingOperation.STOP,
                outcome = WearRecordingOutcome.FAILED,
                errorCode = AppErrorCode.RECORDING,
                errorOperation = "Stop watch recording",
                errorMessage = "watch recording service could not stop",
                errorContext = mapOf("serviceState" to "stopping"),
                failureCount = 2,
            ),
        )

        commands.forEach { command ->
            val payload = WearCommandCodec.encode(command).getOrThrow()
            assertEquals(command, WearCommandCodec.decode(payload).getOrThrow())
        }
    }

    @Test
    fun `Given trailing bytes When decoded Then payload is rejected`() {
        val valid = WearCommandCodec.encode(WearCommand.LaunchPhone).getOrThrow()

        assertTrue(WearCommandCodec.decode(valid + byteArrayOf(99)).isFailure)
    }

    @Test
    fun `Given a truncated current payload When decoded Then it is rejected`() {
        val malformed = byteArrayOf(0x53, 0x42, 0x58, 0x34, 0x04)

        assertTrue(WearCommandCodec.decode(malformed).isFailure)
    }

    @Test
    fun `Given a successful result with an error When encoded Then it is rejected`() {
        val invalid = WearCommand.RecordingResult(
            sessionId = "session-123",
            operation = WearRecordingOperation.START,
            outcome = WearRecordingOutcome.SUCCEEDED,
            errorCode = AppErrorCode.RECORDING,
            errorOperation = "Unexpected failure",
            errorMessage = "A successful result cannot contain an error",
        )

        assertTrue(WearCommandCodec.encode(invalid).isFailure)
    }

    @Test
    fun `Given a malformed session identifier When encoded Then it is rejected`() {
        val invalid = WearCommand.StopRecording(
            "session/with/private/path",
            WearStopReason.USER_REQUEST,
        )

        assertTrue(WearCommandCodec.encode(invalid).isFailure)
    }
}
