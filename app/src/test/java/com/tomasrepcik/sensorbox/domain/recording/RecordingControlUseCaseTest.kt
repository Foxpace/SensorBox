package com.tomasrepcik.sensorbox.domain.recording

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.wearoslib.protocol.RecordingCommandSender
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearStopReason
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingControlUseCaseTest {
    @Test
    fun `Given phone sources only When recording starts Then no watch command is sent`() = runTest {
        // Given
        val fixture = Fixture()

        // When
        val result = fixture.user.start(request())

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.phone.startCalls)
        assertTrue(fixture.watch.commands.isEmpty())
    }

    @Test
    fun `Given watch sources When recording starts Then phone starts before direct watch start`() = runTest {
        // Given
        val fixture = Fixture()

        // When
        val result = fixture.user.start(request(watchSensorIds = setOf(1)))

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.phone.startCalls)
        assertTrue(fixture.watch.commands.single() is WearCommand.StartRecording)
    }

    @Test
    fun `Given timed paired recording When recording starts Then both devices receive the same duration`() = runTest {
        // Given
        val fixture = Fixture()

        // When
        fixture.user.start(request(watchSensorIds = setOf(1), durationSeconds = 12))

        // Then
        val watchStart = fixture.watch.commands.single() as WearCommand.StartRecording
        assertEquals(12_000L, fixture.phone.startedDurationMillis)
        assertEquals(fixture.phone.startedDurationMillis, watchStart.request.durationMillis)
    }

    @Test
    fun `Given watch start rejection When recording starts Then phone and watch are stopped`() = runTest {
        // Given
        val fixture = Fixture().apply {
            watch.startResult = AppResult.failure(AppError(AppErrorCode.RECORDING, "Start watch recording"))
        }

        // When
        val result = fixture.user.start(request(watchSensorIds = setOf(1)))

        // Then
        assertTrue(result.isFailure)
        assertEquals(1, fixture.phone.stopCalls)
        assertTrue(fixture.watch.commands.last() is WearCommand.StopRecording)
    }

    @Test
    fun `Given watch start delivery without a result When recording starts Then phone keeps recording`() = runTest {
        // Given
        val fixture = Fixture().apply {
            watch.startResult = AppResult.failure(
                AppError(
                    code = AppErrorCode.TIMEOUT,
                    operation = "Await watch start result",
                    diagnosticMessage = "Fixture result was lost",
                    context = mapOf("commandWasSent" to "true"),
                ),
            )
        }

        // When
        val result = fixture.user.start(request(watchSensorIds = setOf(1), durationSeconds = 12))

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.phone.startCalls)
        assertEquals(0, fixture.phone.stopCalls)
    }

    @Test
    fun `Given paired recording When stopped Then phone and watch receive stop`() = runTest {
        // Given
        val fixture = Fixture()
        fixture.user.start(request(watchSensorIds = setOf(1)))

        // When
        val result = fixture.user.stop()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, fixture.phone.stopCalls)
        assertTrue(fixture.watch.commands.last() is WearCommand.StopRecording)
    }

    @Test
    fun `Given active recording When another start is requested Then intent is rejected`() = runTest {
        // Given
        val fixture = Fixture()
        fixture.user.start(request())

        // When
        val result = fixture.user.start(request())

        // Then
        assertEquals(AppErrorCode.CONFLICT, result.errorOrNull()?.code)
        assertEquals(1, fixture.phone.startCalls)
    }

    @Test
    fun `Given active recording When stale watch stop arrives Then phone keeps recording`() = runTest {
        // Given
        val fixture = Fixture()
        fixture.user.start(request())

        // When
        val result = fixture.peer.stopFromWatch("stale-session", WearStopReason.USER_REQUEST)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, fixture.phone.stopCalls)
    }

    @Test
    fun `Given automatic phone stop When watch is unreachable Then the next recording can start`() = runTest {
        // Given
        val fixture = Fixture()
        fixture.user.start(request(watchSensorIds = setOf(1)))
        val sessionId = (fixture.watch.commands.single() as WearCommand.StartRecording).sessionId
        fixture.watch.stopResult = AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Stop watch recording"))

        // When
        val peerNotification = fixture.phoneEvents.onAutomaticPhoneStop(
            sessionId,
            WearStopReason.DURATION_EXPIRED,
        )
        val nextStart = fixture.user.start(request())

        // Then
        assertTrue(peerNotification.isFailure)
        assertTrue(nextStart.isSuccess)
        assertEquals(2, fixture.phone.startCalls)
    }

    @Test
    fun `Given an annotation When recording controls it Then the current time reaches the phone`() {
        // Given
        val fixture = Fixture()

        // When
        fixture.user.annotate("fixture note")

        // Then
        assertEquals("fixture note", fixture.phone.annotation)
        assertEquals(12_345L, fixture.phone.annotationTimeMillis)
    }

    private fun request(watchSensorIds: Set<Int> = emptySet(), durationSeconds: Int = 0) = RecordingSetup(
        sensorIds = setOf(1),
        includesGps = false,
        samplingPeriodIndex = 0,
        stopOnLowBattery = false,
        useWakeLock = false,
        gpsIntervalSeconds = 10,
        gpsMinDistanceMeters = 20,
        watchSensorIds = watchSensorIds,
        durationSeconds = durationSeconds,
    )

    private class Fixture {
        val phone = FakePhoneRecordingController()
        val watch = FakeRecordingCommandSender()
        private val control = DefaultRecordingControlUseCase(
            phoneRecording = phone,
            watchCommands = watch,
            diagnosticLogger = DiagnosticLogger { },
            clock = EpochClock { 12_345L },
        )
        val user: RecordingControlUseCase = control
        val phoneEvents: PhoneRecordingSessionControl = control
        val peer: PeerRecordingControl = control
    }
}

private class FakePhoneRecordingController : PhoneRecordingController {
    var startCalls = 0
    var stopCalls = 0
    var startedDurationMillis = 0L
    var annotation = ""
    var annotationTimeMillis = 0L

    override fun start(sessionId: String, request: RecordingSetup): AppResult<StartedPhoneRecording> {
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

    override fun annotate(text: String, timestampMillis: Long): AppResult<Unit> {
        annotation = text
        annotationTimeMillis = timestampMillis
        return AppResult.success(Unit)
    }
}

private class FakeRecordingCommandSender : RecordingCommandSender {
    val commands = mutableListOf<WearCommand>()
    var startResult: AppResult<Unit> = AppResult.success(Unit)
    var stopResult: AppResult<Unit> = AppResult.success(Unit)

    override suspend fun send(command: WearCommand): AppResult<Unit> {
        commands += command
        return when (command) {
            is WearCommand.StartRecording -> startResult
            is WearCommand.StopRecording -> stopResult
            else -> error("Fixture only accepts recording commands")
        }
    }
}
