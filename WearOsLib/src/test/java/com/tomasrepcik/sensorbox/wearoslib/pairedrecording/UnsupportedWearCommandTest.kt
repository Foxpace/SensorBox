package com.tomasrepcik.sensorbox.wearoslib.pairedrecording

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertTrue
import org.junit.Test

class UnsupportedWearCommandTest {
    @Test
    fun `Given an older protocol When decoded Then it is rejected`() {
        // Given
        val current = WearCommandCodec.encode(WearCommand.LaunchPhone).getOrThrow().decodeToString()
        val older = current.replace("\"protocolVersion\":${WearCommandCodec.PROTOCOL_VERSION}", "\"protocolVersion\":6")

        // When
        val result = WearCommandCodec.decode(older.encodeToByteArray())

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `Given an unscoped sync command When decoded Then it is rejected`() {
        // Given
        val current = WearCommandCodec.encode(WearCommand.LaunchPhone).getOrThrow().decodeToString()
        val obsolete = current.replace("LaunchPhone", "SyncMeasurements")

        // When
        val result = WearCommandCodec.decode(obsolete.encodeToByteArray())

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `Given missing recording settings When decoded Then the request is rejected`() {
        // Given
        val request = WearRecordingRequest(
            "walk",
            listOf(1),
            false,
            settings = WearRecordingSettings(0, true, false, 1, 0),
        )
        val payload = WearCommandCodec.encode(WearCommand.StartRecording("session", request)).getOrThrow()
        val envelope = Json.parseToJsonElement(payload.decodeToString()).jsonObject
        val command = envelope.getValue("command").jsonObject
        val incomplete = JsonObject(command.getValue("request").jsonObject - "settings")
        val changed = JsonObject(envelope + ("command" to JsonObject(command + ("request" to incomplete))))

        // When
        val result = WearCommandCodec.decode(changed.toString().encodeToByteArray())

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `Given an obsolete result field When decoded Then it is rejected`() {
        // Given
        val obsolete = failurePayload().replace("\"operation\":", "\"action\":")

        // When
        val result = WearCommandCodec.decode(obsolete.encodeToByteArray())

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `Given an obsolete error name When decoded Then it is rejected`() {
        // Given
        val obsolete = failurePayload().replace("\"RECORDING\"", "\"MEASUREMENT\"")

        // When
        val result = WearCommandCodec.decode(obsolete.encodeToByteArray())

        // Then
        assertTrue(result.isFailure)
    }

    private fun failurePayload(): String = WearCommandCodec.encode(
        WearCommand.RecordingResult(
            sessionId = "session",
            operation = WearRecordingOperation.START,
            outcome = WearRecordingOutcome.FAILED,
            errorCode = AppErrorCode.RECORDING,
            errorOperation = "Start recording",
            errorMessage = "Fixture failure",
        ),
    ).getOrThrow().decodeToString()
}
