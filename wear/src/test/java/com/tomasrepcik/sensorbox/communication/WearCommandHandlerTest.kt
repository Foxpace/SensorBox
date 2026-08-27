package com.tomasrepcik.sensorbox.communication

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.domain.measurement.WearRecordingControlUseCase
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.connectivity.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearNode
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOutcome
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
    fun `Given duplicate start and stop commands When handled Then recording effects run once`() = runTest {
        // Given
        val fixture = Fixture()
        val start = WearCommand.StartRecording("session-123", request())
        val stop = WearCommand.StopRecording("session-123", WearStopReason.USER_REQUEST)

        // When
        fixture.handler.handle(start)
        fixture.handler.handle(start)
        fixture.handler.handle(stop)
        fixture.handler.handle(stop)

        // Then
        assertEquals(1, fixture.requirements.validationCalls)
        assertEquals(1, fixture.recording.startCalls)
        assertEquals(1, fixture.recording.stopCalls)
        assertEquals(fixture.repository.commands[0], fixture.repository.commands[1])
        assertEquals(fixture.repository.commands[2], fixture.repository.commands[3])
    }

    @Test
    fun `Given encoded start command When dispatched Then direct recording starts`() = runTest {
        // Given
        val fixture = Fixture()
        val dispatcher = WearMessageDispatcher(fixture.handler, DiagnosticLogger { })
        val payload = WearCommandCodec.encode(WearCommand.StartRecording("session-123", request())).getOrThrow()

        // When
        val result = dispatcher.dispatch(WEAR_MESSAGE_PATH, payload)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.recording.startCalls)
    }

    @Test
    fun `Given start validation fails When handled Then diagnostic result reaches phone`() = runTest {
        // Given
        val fixture = Fixture()
        fixture.requirements.validationResult = AppResult.failure(
            AppError(
                code = AppErrorCode.PERMISSION,
                operation = "Validate Wear recording permissions",
                diagnosticMessage = "Wear OS is missing required recording permissions",
                context = mapOf("missingPermissions" to "android.permission.ACCESS_FINE_LOCATION"),
            ),
        )

        // When
        fixture.handler.handle(WearCommand.StartRecording("session-123", request()))

        // Then
        val result = fixture.repository.commands.single() as WearCommand.RecordingResult
        assertEquals(WearRecordingOutcome.FAILED, result.outcome)
        assertEquals(AppErrorCode.PERMISSION, result.errorCode)
        assertEquals("Validate Wear recording permissions", result.errorOperation)
        assertEquals(
            "android.permission.ACCESS_FINE_LOCATION",
            result.errorContext["missingPermissions"],
        )
    }

    @Test
    fun `Given sensor discovery command When handled Then available sensors are returned`() = runTest {
        // Given
        val fixture = Fixture()
        fixture.requirements.sensors = listOf(sensor())

        // When
        fixture.handler.handle(WearCommand.RequestAvailableSensors)

        // Then
        assertEquals(WearCommand.AvailableSensors(listOf(sensor())), fixture.repository.commands.single())
    }

    @Test
    fun `Given Wear timer expires while phone is unreachable When recording starts again Then Wear is available`() =
        runTest {
            // Given
            val fixture = Fixture()
            fixture.handler.handle(WearCommand.StartRecording("session-123", request()))
            fixture.repository.failSends = true

            // When
            val peerNotification = fixture.handler.onAutomaticStop(WearStopReason.DURATION_EXPIRED)
            fixture.repository.failSends = false
            val nextStart = fixture.handler.handle(WearCommand.StartRecording("session-456", request()))

            // Then
            assertTrue(peerNotification.isFailure)
            assertTrue(nextStart.isSuccess)
            assertEquals(0, fixture.recording.stopCalls)
            assertEquals(2, fixture.recording.startCalls)
        }

    private fun request() = WearRecordingRequest("fixture", listOf(1), includesGps = false)

    private fun sensor() = WearSensorInfo(1, "Accelerometer", "Fixture", 1, "sensor", 1f, 1f, 1f, 1, 1, 0, false)

    private class Fixture {
        val repository = CapturingRepository()
        val recording = FakeWearRecordingControl()
        val requirements = FakeWearRecordingRequirements()
        val handler = WearCommandHandler(
            recording = recording,
            environment = requirements,
            sendCommand = SendWearCommandUseCase(SendWearMessageUseCase(repository)),
            phoneResults = PhoneRecordingResultInbox(),
        )
    }
}

private class FakeWearRecordingControl : WearRecordingControlUseCase {
    var startCalls = 0
    var stopCalls = 0

    override fun start(
        sessionId: String,
        request: WearRecordingRequest,
        preferences: AppPreferences,
    ): AppResult<Unit> {
        startCalls += 1
        return AppResult.success(Unit)
    }

    override fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit> {
        stopCalls += 1
        return AppResult.success(Unit)
    }
}

private class FakeWearRecordingRequirements : WearRecordingRequirementsUseCase {
    var validationCalls = 0
    var validationResult: AppResult<AppPreferences> = AppResult.success(AppPreferences())
    var sensors: List<WearSensorInfo> = emptyList()

    override suspend fun validate(request: WearRecordingRequest): AppResult<AppPreferences> {
        validationCalls += 1
        return validationResult
    }

    override fun availableSensors(): List<WearSensorInfo> = sensors
}

private class CapturingRepository : WearConnectionRepository {
    val commands = mutableListOf<WearCommand>()
    var failSends = false

    override fun observeCapability(capability: String): Flow<WearConnection> = emptyFlow()

    override suspend fun findNode(capability: String): WearNode? = WearNode("phone", "Phone", isNearby = true)

    override suspend fun sendMessage(capability: String, path: String, payload: ByteArray): AppResult<Unit> {
        if (failSends) {
            return AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Send phone command"))
        }

        commands += WearCommandCodec.decode(payload).getOrThrow()
        return AppResult.success(Unit)
    }
}
