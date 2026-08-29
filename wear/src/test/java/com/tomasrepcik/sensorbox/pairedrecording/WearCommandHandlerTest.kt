package com.tomasrepcik.sensorbox.pairedrecording

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.recording.WatchRecordingControlUseCase
import com.tomasrepcik.sensorbox.sync.SyncWatchMeasurementsUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.connection.WearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connection.WearNode
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.RecordingCommandExchange
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingOutcome
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearSensorInfo
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearStopReason
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
                operation = "Validate watch recording permissions",
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
        assertEquals("Validate watch recording permissions", result.errorOperation)
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
    fun `Given a sync command When handled Then watch measurements are sent`() = runTest {
        // Given
        val fixture = Fixture()

        // When
        val result = fixture.handler.handle(WearCommand.SyncMeasurements)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.syncCalls)
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
        var syncCalls = 0
        private val sendCommand = SendWearCommandUseCase(SendWearMessageUseCase(repository))
        private val exchange = RecordingCommandExchange(
            sendCommand = sendCommand,
            peerCapability = "phone-capability",
            peerPath = "/phone",
            peerName = "phone",
        )
        val handler = WearCommandHandler(
            recording = recording,
            environment = requirements,
            sendCommand = sendCommand,
            recordingCommands = exchange,
            recordingResults = exchange,
            syncMeasurements = SyncWatchMeasurementsUseCase {
                syncCalls += 1
                AppResult.success(1)
            },
        )
    }
}

private class FakeWearRecordingControl : WatchRecordingControlUseCase {
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
