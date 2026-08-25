package com.tomasrepcik.sensorbox.wearoslib.protocol

import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearCommandCodecTest {
    @Test
    fun `Given protocol v3 commands When round tripped Then every field survives`() {
        val request = WearRecordingRequest(
            folderName = "shared_session",
            sensorIds = listOf(1, 4, 21),
            includesGps = true,
            durationMillis = 45_000L,
        )
        val commands = listOf(
            WearCommand.LaunchPhone,
            WearCommand.SyncMeasurements,
            WearCommand.RequestSensorList,
            WearCommand.SensorList(listOf(WearSensorInfo(1, "Accelerometer", "Fixture"))),
            WearCommand.PrepareRecording("session-123", request),
            WearCommand.CommitRecording("session-123", 1_800_000_000_000L),
            WearCommand.AbortRecording("session-123"),
            WearCommand.StopRecording("session-123", WearStopReason.LOW_BATTERY),
            WearCommand.Acknowledgement(
                sessionId = "session-123",
                command = WearSessionCommand.PREPARE,
                outcome = WearAcknowledgementOutcome.SUCCEEDED,
            ),
            WearCommand.Acknowledgement(
                sessionId = "session-123",
                command = WearSessionCommand.STOP,
                outcome = WearAcknowledgementOutcome.FAILED,
                errorCode = AppErrorCode.MEASUREMENT,
                failureCount = 2,
            ),
        )

        commands.forEach { command ->
            val payload = WearCommandCodec.encode(command).getOrThrow()
            assertEquals(command, WearCommandCodec.decode(payload).getOrThrow())
        }
    }

    @Test
    fun `Given a protocol v1 header When decoded Then it is rejected`() {
        val v1Payload = byteArrayOf(0x53, 0x42, 0x58, 0x31, 0x01, 0x01)

        assertTrue(WearCommandCodec.decode(v1Payload).isFailure)
    }

    @Test
    fun `Given a protocol v2 header When decoded Then it is rejected`() {
        val v2Payload = byteArrayOf(0x53, 0x42, 0x58, 0x32, 0x02, 0x01)

        assertTrue(WearCommandCodec.decode(v2Payload).isFailure)
    }

    @Test
    fun `Given trailing bytes When decoded Then payload is rejected`() {
        val valid = WearCommandCodec.encode(WearCommand.LaunchPhone).getOrThrow()

        assertTrue(WearCommandCodec.decode(valid + byteArrayOf(99)).isFailure)
    }

    @Test
    fun `Given a truncated v3 payload When decoded Then it is rejected`() {
        val malformed = byteArrayOf(0x53, 0x42, 0x58, 0x33, 0x03)

        assertTrue(WearCommandCodec.decode(malformed).isFailure)
    }

    @Test
    fun `Given a successful acknowledgement with an error When encoded Then it is rejected`() {
        val invalid = WearCommand.Acknowledgement(
            sessionId = "session-123",
            command = WearSessionCommand.COMMIT,
            outcome = WearAcknowledgementOutcome.SUCCEEDED,
            errorCode = AppErrorCode.MEASUREMENT,
        )

        assertTrue(WearCommandCodec.encode(invalid).isFailure)
    }

    @Test
    fun `Given a malformed session identifier When encoded Then it is rejected`() {
        val invalid = WearCommand.AbortRecording("session/with/private/path")

        assertTrue(WearCommandCodec.encode(invalid).isFailure)
    }
}
