package com.tomasrepcik.sensorbox.domain.paired

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.domain.measurement.MeasurementRequest
import com.tomasrepcik.sensorbox.domain.measurement.PhoneRecordingController
import com.tomasrepcik.sensorbox.domain.measurement.StartedPhoneRecording
import com.tomasrepcik.sensorbox.wearoslib.connectivity.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearNode
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingAction
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOutcome
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearStopReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairedRecordingCoordinatorTest {
    @Test
    fun `Given phone sources only When recording starts Then no Wear command is sent`() = runTest {
        // Given
        val fixture = Fixture()

        // When
        val result = fixture.coordinator.start(request())

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.phone.startCalls)
        assertTrue(fixture.repository.commands.isEmpty())
    }

    @Test
    fun `Given Wear sources When recording starts Then phone starts before direct Wear start`() = runTest {
        // Given
        val fixture = Fixture()

        // When
        val result = fixture.coordinator.start(request(wearSensorIds = setOf(1)))

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.phone.startCalls)
        assertTrue(fixture.repository.commands.single() is WearCommand.StartRecording)
    }

    @Test
    fun `Given timed paired recording When recording starts Then both devices receive the same duration`() = runTest {
        // Given
        val fixture = Fixture()

        // When
        val result = fixture.coordinator.start(
            request(wearSensorIds = setOf(1), durationSeconds = 12),
        )

        // Then
        val wearStart = fixture.repository.commands.single() as WearCommand.StartRecording
        assertTrue(result.isSuccess)
        assertEquals(12_000L, fixture.phone.startedDurationMillis)
        assertEquals(fixture.phone.startedDurationMillis, wearStart.request.durationMillis)
    }

    @Test
    fun `Given Wear start fails When recording starts Then phone and Wear are stopped`() = runTest {
        // Given
        val fixture = Fixture().apply { repository.failStart = true }

        // When
        val result = fixture.coordinator.start(request(wearSensorIds = setOf(1)))

        // Then
        assertTrue(result.isFailure)
        assertEquals(1, fixture.phone.stopCalls)
        assertTrue(fixture.repository.commands.last() is WearCommand.StopRecording)
    }

    @Test
    fun `Given Wear start result is lost When recording starts Then phone keeps recording`() = runTest {
        // Given
        val fixture = Fixture().apply { repository.dropStartResult = true }

        // When
        val result = fixture.coordinator.start(
            request(wearSensorIds = setOf(1), durationSeconds = 12),
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.phone.startCalls)
        assertEquals(0, fixture.phone.stopCalls)
        assertEquals(3, fixture.repository.commands.count { it is WearCommand.StartRecording })
    }

    @Test
    fun `Given paired recording When stopped Then phone and Wear receive stop`() = runTest {
        // Given
        val fixture = Fixture()
        fixture.coordinator.start(request(wearSensorIds = setOf(1)))

        // When
        val result = fixture.coordinator.stop()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.phone.stopCalls)
        assertTrue(fixture.repository.commands.last() is WearCommand.StopRecording)
    }

    @Test
    fun `Given active recording When another start is requested Then intent is rejected`() = runTest {
        // Given
        val fixture = Fixture()
        fixture.coordinator.start(request())

        // When
        val result = fixture.coordinator.start(request())

        // Then
        assertTrue(result.isFailure)
        assertEquals(AppErrorCode.CONFLICT, result.errorOrNull()?.code)
        assertEquals(1, fixture.phone.startCalls)
    }

    @Test
    fun `Given active recording When stale Wear stop arrives Then phone keeps recording`() = runTest {
        // Given
        val fixture = Fixture()
        fixture.coordinator.start(request())

        // When
        val result = fixture.coordinator.stopFromWatch("stale-session", WearStopReason.USER_REQUEST)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, fixture.phone.stopCalls)
    }

    @Test
    fun `Given phone timer expires while Wear is unreachable When recording starts again Then phone is available`() =
        runTest {
            // Given
            val fixture = Fixture()
            fixture.coordinator.start(request(wearSensorIds = setOf(1), durationSeconds = 12))
            val sessionId = (fixture.repository.commands.single() as WearCommand.StartRecording).sessionId
            fixture.repository.failSends = true

            // When
            val peerNotification = fixture.coordinator.onAutomaticPhoneStop(
                sessionId,
                WearStopReason.DURATION_EXPIRED,
            )
            fixture.repository.failSends = false
            val nextStart = fixture.coordinator.start(request())

            // Then
            assertTrue(peerNotification.isFailure)
            assertTrue(nextStart.isSuccess)
            assertEquals(0, fixture.phone.stopCalls)
            assertEquals(2, fixture.phone.startCalls)
        }

    private fun request(wearSensorIds: Set<Int> = emptySet(), durationSeconds: Int = 0) = MeasurementRequest(
        sensorIds = setOf(1),
        includesGps = false,
        samplingPeriodIndex = 0,
        stopOnLowBattery = false,
        useWakeLock = false,
        gpsIntervalSeconds = 10,
        gpsMinDistanceMeters = 20,
        wearSensorIds = wearSensorIds,
        durationSeconds = durationSeconds,
    )

    private class Fixture {
        val inbox = WearRecordingResultInbox()
        val phone = FakePhoneRecordingController()
        val repository = RecordingResultRepository(inbox)
        val coordinator = PairedRecordingCoordinator(
            phoneRecording = phone,
            sendCommand = SendWearCommandUseCase(SendWearMessageUseCase(repository)),
            watchResults = inbox,
            diagnosticLogger = DiagnosticLogger { },
        )
    }
}

private class FakePhoneRecordingController : PhoneRecordingController {
    var startCalls = 0
    var stopCalls = 0
    var startedDurationMillis = 0L

    override fun start(sessionId: String, request: MeasurementRequest): AppResult<StartedPhoneRecording> {
        startCalls += 1
        startedDurationMillis = request.durationSeconds.coerceAtLeast(0) * 1_000L
        return AppResult.success(StartedPhoneRecording(sessionId, "fixture", startedDurationMillis))
    }

    override fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit> {
        stopCalls += 1
        return AppResult.success(Unit)
    }

    override fun stopCurrent(reason: WearStopReason): AppResult<Unit> {
        stopCalls += 1
        return AppResult.success(Unit)
    }

    override fun annotate(text: String, timestampMillis: Long): AppResult<Unit> = AppResult.success(Unit)
}

private class RecordingResultRepository(private val inbox: WearRecordingResultInbox) : WearConnectionRepository {
    val commands = mutableListOf<WearCommand>()
    var failStart = false
    var failSends = false
    var dropStartResult = false

    override fun observeCapability(capability: String): Flow<WearConnection> = emptyFlow()

    override suspend fun findNode(capability: String): WearNode? = WearNode("watch", "Watch", isNearby = true)

    override suspend fun sendMessage(capability: String, path: String, payload: ByteArray): AppResult<Unit> {
        if (failSends) {
            return AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Send Wear command"))
        }

        val command = WearCommandCodec.decode(payload).getOrThrow()
        commands += command
        when (command) {
            is WearCommand.StartRecording -> if (!dropStartResult) {
                publish(
                    command.sessionId,
                    WearRecordingAction.START,
                    failed = failStart,
                )
            }

            is WearCommand.StopRecording -> publish(command.sessionId, WearRecordingAction.STOP, failed = false)

            else -> Unit
        }
        return AppResult.success(Unit)
    }

    private fun publish(sessionId: String, action: WearRecordingAction, failed: Boolean) {
        inbox.publish(
            WearCommand.RecordingResult(
                sessionId = sessionId,
                action = action,
                outcome = if (failed) WearRecordingOutcome.FAILED else WearRecordingOutcome.SUCCEEDED,
                errorCode = AppErrorCode.MEASUREMENT.takeIf { failed },
                errorOperation = "Start Wear recording".takeIf { failed },
                errorMessage = "Fixture start failed".takeIf { failed },
                failureCount = if (failed) 1 else 0,
            ),
        )
    }
}
