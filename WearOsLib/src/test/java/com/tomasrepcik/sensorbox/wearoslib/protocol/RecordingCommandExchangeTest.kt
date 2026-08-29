package com.tomasrepcik.sensorbox.wearoslib.protocol

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.wearoslib.connectivity.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingCommandExchangeTest {
    @Test
    fun `Given a recording result When a command is sent Then its outcome is returned`() = runTest {
        // Given
        val fixture = Fixture()

        // When
        val result = fixture.exchange.send(fixture.startCommand())

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.repository.sendCalls)
    }

    @Test
    fun `Given a failed peer result When a command is sent Then peer diagnostics are retained`() = runTest {
        // Given
        val fixture = Fixture().apply { resultFails = true }

        // When
        val result = fixture.exchange.send(fixture.startCommand())

        // Then
        assertEquals(AppErrorCode.PERMISSION, result.errorOrNull()?.code)
        assertEquals("watch", result.errorOrNull()?.context?.get("source"))
        assertEquals("fixture", result.errorOrNull()?.context?.get("missingPermissions"))
    }

    @Test
    fun `Given a lost result When a command is sent Then delivery retries and reports that it was sent`() = runTest {
        // Given
        val fixture = Fixture().apply { dropResults = true }

        // When
        val result = fixture.exchange.send(fixture.startCommand())

        // Then
        assertEquals(3, fixture.repository.sendCalls)
        assertEquals(AppErrorCode.TIMEOUT, result.errorOrNull()?.code)
        assertEquals("true", result.errorOrNull()?.context?.get("commandWasSent"))
    }

    @Test
    fun `Given transport failures When a command is sent Then the last transport error is returned`() = runTest {
        // Given
        val fixture = Fixture().apply { sendFails = true }

        // When
        val result = fixture.exchange.send(fixture.startCommand())

        // Then
        assertEquals(3, fixture.repository.sendCalls)
        assertEquals(AppErrorCode.CONNECTIVITY, result.errorOrNull()?.code)
    }

    private class Fixture {
        val repository = ExchangeRepository()
        val exchange = RecordingCommandExchange(
            sendCommand = SendWearCommandUseCase(SendWearMessageUseCase(repository)),
            peerCapability = "watch-capability",
            peerPath = "/watch",
            peerName = "watch",
        )
        var dropResults = false
        var resultFails = false
        var sendFails: Boolean
            get() = repository.sendFails
            set(value) {
                repository.sendFails = value
            }

        init {
            repository.onCommand = { command ->
                if (!dropResults) exchange.receive(command.result(resultFails))
            }
        }

        fun startCommand() = WearCommand.StartRecording(
            sessionId = "session-123",
            request = WearRecordingRequest("fixture", listOf(1), includesGps = false),
        )

        private fun WearCommand.result(failed: Boolean): WearCommand.RecordingResult {
            val operation = when (this) {
                is WearCommand.StartRecording -> WearRecordingOperation.START
                is WearCommand.StopRecording -> WearRecordingOperation.STOP
                else -> error("Fixture only accepts recording commands")
            }
            return WearCommand.RecordingResult(
                sessionId = checkNotNull(recordingSessionId()),
                operation = operation,
                outcome = if (failed) WearRecordingOutcome.FAILED else WearRecordingOutcome.SUCCEEDED,
                errorCode = AppErrorCode.PERMISSION.takeIf { failed },
                errorOperation = "Validate watch recording".takeIf { failed },
                errorMessage = "Fixture permission failure".takeIf { failed },
                errorContext = if (failed) mapOf("missingPermissions" to "fixture") else emptyMap(),
                failureCount = if (failed) 1 else 0,
            )
        }
    }
}

private class ExchangeRepository : WearConnectionRepository {
    var sendCalls = 0
    var sendFails = false
    var onCommand: (WearCommand) -> Unit = { }

    override fun observeCapability(capability: String): Flow<WearConnection> = emptyFlow()

    override suspend fun findNode(capability: String): WearNode = WearNode("peer", "Peer", isNearby = true)

    override suspend fun sendMessage(capability: String, path: String, payload: ByteArray): AppResult<Unit> {
        sendCalls += 1
        if (sendFails) return AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Send fixture command"))
        onCommand(WearCommandCodec.decode(payload).getOrThrow())
        return AppResult.success(Unit)
    }
}
