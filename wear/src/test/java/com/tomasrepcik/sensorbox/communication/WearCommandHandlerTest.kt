package com.tomasrepcik.sensorbox.communication

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.domain.measurement.WearRecordingController
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.connectivity.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearNode
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearSensorInfo
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearStopReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearCommandHandlerTest {
    @Test
    fun `Given duplicate session commands When handled Then side effects run once and acknowledgements repeat`() =
        runTest {
            val fixture = Fixture()
            val prepare = WearCommand.PrepareRecording("session-123", request())
            val commit = WearCommand.CommitRecording("session-123", 1_800_000_000_000L)
            val stop = WearCommand.StopRecording("session-123", WearStopReason.USER_REQUEST)

            fixture.handler.handle(prepare)
            fixture.handler.handle(prepare)
            fixture.handler.handle(commit)
            fixture.handler.handle(commit)
            fixture.handler.handle(stop)
            fixture.handler.handle(stop)

            assertEquals(1, fixture.environment.prepareCalls)
            assertEquals(1, fixture.controller.startCalls)
            assertEquals(1, fixture.controller.stopCalls)
            assertEquals(fixture.repository.commands[0], fixture.repository.commands[1])
            assertEquals(fixture.repository.commands[2], fixture.repository.commands[3])
            assertEquals(fixture.repository.commands[4], fixture.repository.commands[5])
        }

    @Test
    fun `Given encoded command bytes When dispatched Then no Google callback type is required`() = runTest {
        val fixture = Fixture()
        val dispatcher = WearMessageDispatcher(fixture.handler, DiagnosticLogger { })
        val payload = WearCommandCodec.encode(WearCommand.PrepareRecording("session-123", request())).getOrThrow()

        val result = dispatcher.dispatch(WEAR_MESSAGE_PATH, payload)

        assertTrue(result.isSuccess)
        assertEquals(1, fixture.environment.prepareCalls)
    }

    @Test
    fun `Given an old protocol payload When dispatched Then it is rejected before policy runs`() = runTest {
        val fixture = Fixture()
        val dispatcher = WearMessageDispatcher(fixture.handler, DiagnosticLogger { })

        val result = dispatcher.dispatch(WEAR_MESSAGE_PATH, byteArrayOf(0x53, 0x42, 0x58, 0x31, 1, 1))

        assertTrue(result.isFailure)
        assertEquals(0, fixture.environment.prepareCalls)
    }

    private fun request() = WearRecordingRequest(
        folderName = "fixture",
        sensorIds = listOf(1),
        includesGps = false,
    )

    private class Fixture {
        val repository = CapturingRepository()
        val controller = FakeWearRecordingController()
        val environment = FakeWearCommandEnvironment()
        val handler = WearCommandHandler(
            recordingController = controller,
            environment = environment,
            sendCommand = SendWearCommandUseCase(SendWearMessageUseCase(repository)),
            acknowledgementInbox = WearAcknowledgementInbox(),
        )
    }
}

private class FakeWearRecordingController : WearRecordingController {
    var startCalls = 0
    var stopCalls = 0

    override fun start(
        sessionId: String,
        request: WearRecordingRequest,
        preferences: AppPreferences,
        startAtEpochMillis: Long,
    ): AppResult<Unit> {
        startCalls += 1
        return AppResult.success(Unit)
    }

    override fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit> {
        stopCalls += 1
        return AppResult.success(Unit)
    }
}

private class FakeWearCommandEnvironment : WearCommandEnvironment {
    var prepareCalls = 0

    override suspend fun prepare(request: WearRecordingRequest): AppResult<AppPreferences> {
        prepareCalls += 1
        return AppResult.success(AppPreferences())
    }

    override fun sensors(): List<WearSensorInfo> = emptyList()
}

private class CapturingRepository : WearConnectionRepository {
    val commands = mutableListOf<WearCommand>()

    override fun observeCapability(capability: String): Flow<WearConnection> = emptyFlow()

    override suspend fun findNode(capability: String): WearNode? = WearNode("phone", "Phone", isNearby = true)

    override suspend fun sendMessage(capability: String, path: String, payload: ByteArray): AppResult<Unit> {
        commands += WearCommandCodec.decode(payload).getOrThrow()
        return AppResult.success(Unit)
    }
}
