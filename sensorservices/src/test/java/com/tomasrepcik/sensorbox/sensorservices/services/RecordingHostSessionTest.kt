package com.tomasrepcik.sensorbox.sensorservices.services

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticEvent
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.recording.RecordingEvent
import com.tomasrepcik.sensorbox.recording.RecordingSessionId
import com.tomasrepcik.sensorbox.recording.RecordingStopReason
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStopReason
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingHostSessionTest {
    @Test
    fun `Given a recording request When execution starts Then running state and alarms are published`() = runTest {
        // Given
        val fixture = Fixture(backgroundScope)

        // When
        val result = fixture.session.start()
        runCurrent()
        fixture.execution.emit(RecordingEvent.RecordingStarted(RecordingSessionId("session-123")))
        runCurrent()

        // Then
        assertTrue(result.isSuccess)
        val running = fixture.store.state.value as RecordingSessionState.Running
        assertEquals("session-123", running.sessionId)
        assertEquals(55_000L, running.startedAtElapsedRealtime)
        assertEquals(listOf(4, 8), fixture.environment.scheduledAlarms)
    }

    @Test
    fun `Given execution start failure When session starts Then host finishes and publishes failure`() = runTest {
        // Given
        val fixture = Fixture(backgroundScope).apply {
            execution.startResult = AppResult.failure(AppError(AppErrorCode.RECORDING, "Start fixture execution"))
        }
        val stopped = async { fixture.store.events.first() }
        runCurrent()

        // When
        val result = fixture.session.start()

        // Then
        assertTrue(result.isFailure)
        assertEquals(RecordingSessionStopReason.SOURCE_FAILURE, stopped.await().reason)
        assertEquals(1, fixture.environment.releaseCalls)
        assertEquals(1, fixture.environment.removeNotificationCalls)
        assertEquals(1, fixture.environment.stopServiceCalls)
    }

    @Test
    fun `Given execution stops twice When events arrive Then host cleanup runs once`() = runTest {
        // Given
        val fixture = Fixture(backgroundScope)
        fixture.session.start()
        runCurrent()
        val stopped = async { fixture.store.events.first() }
        runCurrent()
        val event = RecordingEvent.RecordingStopped(
            sessionId = RecordingSessionId("session-123"),
            reason = RecordingStopReason.DURATION_EXPIRED,
            result = AppResult.success(Unit),
        )

        // When
        fixture.execution.emit(event)
        runCurrent()
        fixture.execution.emit(event)
        runCurrent()

        // Then
        assertEquals(RecordingSessionStopReason.DURATION_EXPIRED, stopped.await().reason)
        assertTrue(fixture.store.state.value is RecordingSessionState.Idle)
        assertEquals(1, fixture.environment.releaseCalls)
        assertEquals(1, fixture.environment.stopServiceCalls)
    }

    @Test
    fun `Given a recording source fails When execution continues Then failures are stored in diagnostics`() = runTest {
        // Given
        val fixture = Fixture(backgroundScope)
        fixture.session.start()
        runCurrent()
        fixture.execution.emit(RecordingEvent.RecordingStarted(RecordingSessionId("session-123")))
        runCurrent()
        val sourceFailure = AppError(AppErrorCode.RECORDING, "Record SENSOR")
        val stopFailure = AppError(AppErrorCode.RECORDING, "Stop SENSOR")

        // When
        fixture.execution.emit(
            RecordingEvent.SourceFailed(
                sessionId = RecordingSessionId("session-123"),
                sourceType = com.tomasrepcik.sensorbox.recording.RecordingSourceType.SENSOR,
                failure = sourceFailure,
                stopFailure = stopFailure,
            ),
        )
        runCurrent()

        // Then
        assertEquals(listOf("Record SENSOR", "Stop SENSOR"), fixture.diagnostics.map(DiagnosticEvent::operation))
        assertTrue(fixture.store.state.value !is RecordingSessionState.Idle)
        assertEquals(0, fixture.environment.releaseCalls)
    }

    @Test
    fun `Given an active host When service is destroyed Then execution and resources are released`() = runTest {
        // Given
        val fixture = Fixture(backgroundScope)
        fixture.session.start()
        runCurrent()
        val stopped = async { fixture.store.events.first() }
        runCurrent()

        // When
        fixture.session.destroy()

        // Then
        assertEquals(RecordingStopReason.PLATFORM_DESTROYED, fixture.execution.stopReasons.single())
        assertEquals(RecordingSessionStopReason.SERVICE_DESTROYED, stopped.await().reason)
        assertEquals(1, fixture.environment.releaseCalls)
        assertEquals(0, fixture.environment.stopServiceCalls)
    }

    private class Fixture(scope: kotlinx.coroutines.CoroutineScope) {
        val execution = FakeRecordingSessionExecution()
        val environment = FakeRecordingHostEnvironment()
        val store = RecordingSessionStore()
        val diagnostics = mutableListOf<DiagnosticEvent>()
        val session = RecordingHostSession(
            request = request(),
            execution = execution,
            environment = environment,
            sessionStore = store,
            diagnosticLogger = DiagnosticLogger(diagnostics::add),
            scope = scope,
            elapsedRealtimeMillis = { 55_000L },
        )

        private fun request() = RecordingRequest(
            sessionId = "session-123",
            folderName = "fixture",
            useInternalStorage = true,
            sensorIds = setOf(1),
            sensorSamplingPeriod = 0,
            includesGps = false,
            stopOnLowBattery = false,
            useWakeLock = false,
            gpsIntervalSeconds = 10,
            gpsMinDistanceMeters = 20,
            alarmOffsetsSeconds = listOf(4, 8),
        )
    }
}

private class FakeRecordingSessionExecution : RecordingSessionExecution {
    private val mutableEvents = MutableSharedFlow<RecordingEvent>(extraBufferCapacity = 8)
    override val events: SharedFlow<RecordingEvent> = mutableEvents
    var startResult: AppResult<Unit> = AppResult.success(Unit)
    val stopReasons = mutableListOf<RecordingStopReason>()

    override suspend fun start(): AppResult<Unit> = startResult

    override suspend fun stop(reason: RecordingStopReason): AppResult<Unit> {
        stopReasons += reason
        return AppResult.success(Unit)
    }

    override fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = AppResult.success(Unit)

    override fun playAlarm(): AppResult<Unit> = AppResult.success(Unit)

    fun emit(event: RecordingEvent) {
        mutableEvents.tryEmit(event)
    }
}

private class FakeRecordingHostEnvironment : RecordingHostEnvironment {
    var scheduledAlarms: List<Int> = emptyList()
    var releaseCalls = 0
    var removeNotificationCalls = 0
    var stopServiceCalls = 0

    override fun start(request: RecordingRequest) = Unit

    override fun scheduleAlarms(offsetsSeconds: List<Int>) {
        scheduledAlarms = offsetsSeconds
    }

    override fun release(): AppResult<Unit> {
        releaseCalls += 1
        return AppResult.success(Unit)
    }

    override fun removeNotification() {
        removeNotificationCalls += 1
    }

    override fun stopService() {
        stopServiceCalls += 1
    }
}
