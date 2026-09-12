package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingEngineTest {
    @Test
    fun `Given no active recording When stop is requested Then conflict is returned`() = runTest {
        // Given
        val engine = engine()

        // When
        val result = engine.stop(RecordingStopReason.USER_REQUEST)

        // Then
        assertEquals(AppErrorCode.CONFLICT, result.errorOrNull()?.code)
    }

    @Test
    fun `Given unordered sources When recording starts Then sources start in stable order`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = RecordingEngine(
            sources = listOf(
                FakeSource(RecordingSourceType.GPS, calls),
                FakeSource(RecordingSourceType.SENSOR, calls),
            ),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            waitFor = { },
            sourceStartDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler),
        )
        val request = request(
            RecordingSourceSpec.Gps(10, 20),
            RecordingSourceSpec.Sensors(setOf(1), 0),
        )

        // When
        val result = engine.start(request)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(calls.isEmpty())
        runCurrent()
        assertEquals(listOf("start:SENSOR", "start:GPS"), calls)
    }

    @Test
    fun `Given one source start fails When recording starts Then other source keeps recording`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = engine(
            FakeSource(RecordingSourceType.GPS, calls, startFails = true),
            FakeSource(RecordingSourceType.SENSOR, calls),
        )
        val request = request(
            RecordingSourceSpec.Sensors(setOf(1), 0),
            RecordingSourceSpec.Gps(10, 20),
        )
        val failed = async { engine.events.first { it is RecordingEvent.SourceFailed } }
        runCurrent()

        // When
        val result = engine.start(request)
        val event = failed.await() as RecordingEvent.SourceFailed

        // Then
        assertTrue(result.isSuccess)
        assertEquals(RecordingSourceType.GPS, event.sourceType)
        assertEquals(
            listOf("start:SENSOR", "start:GPS", "stop:GPS:SOURCE_FAILURE"),
            calls,
        )
        engine.stop(RecordingStopReason.USER_REQUEST)
    }

    @Test
    fun `Given source start never returns When recording starts Then next source still launches`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = engine(
            FakeSource(RecordingSourceType.SENSOR, calls, hangOnStart = true),
            FakeSource(RecordingSourceType.GPS, calls),
        )
        val request = request(
            RecordingSourceSpec.Sensors(setOf(1), 0),
            RecordingSourceSpec.Gps(10, 20),
        )

        // When
        val result = engine.start(request)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf("start:SENSOR", "start:GPS"),
            calls,
        )
        assertTrue(engine.stop(RecordingStopReason.USER_REQUEST).isSuccess)
    }

    @Test
    fun `Given source cleanup never returns When start fails Then failure is still published`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = RecordingEngine(
            sources = listOf(
                FakeSource(
                    type = RecordingSourceType.SENSOR,
                    calls = calls,
                    startFails = true,
                    hangOnStop = true,
                ),
            ),
            scope = backgroundScope,
            waitFor = { kotlinx.coroutines.delay(it) },
            sourceStartDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler),
        )
        val request = request(RecordingSourceSpec.Sensors(setOf(1), 0))
        val failed = async { engine.events.first { it is RecordingEvent.SourceFailed } }
        runCurrent()

        // When
        val result = engine.start(request)
        runCurrent()
        advanceTimeBy(5_000L)
        runCurrent()
        val event = failed.await() as RecordingEvent.SourceFailed

        // Then
        assertTrue(result.isSuccess)
        assertEquals(
            listOf("start:SENSOR", "stop:SENSOR:SOURCE_FAILURE"),
            calls,
        )
        assertEquals("Stop SENSOR recording source", event.stopFailure?.operation)
        engine.stop(RecordingStopReason.USER_REQUEST)
    }

    @Test
    fun `Given measurement metadata start fails When recording starts Then sensor still launches`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = engine(
            FakeSource(RecordingSourceType.SESSION_METADATA, calls, startFails = true),
            FakeSource(RecordingSourceType.SENSOR, calls),
        )
        val request = request(
            RecordingSourceSpec.SessionMetadata,
            RecordingSourceSpec.Sensors(setOf(1), 0),
        )
        val failed = async { engine.events.first { it is RecordingEvent.SourceFailed } }
        runCurrent()

        // When
        val result = engine.start(request)
        val event = failed.await() as RecordingEvent.SourceFailed

        // Then
        assertTrue(result.isSuccess)
        assertEquals(RecordingSourceType.SESSION_METADATA, event.sourceType)
        assertEquals(
            listOf("start:SESSION_METADATA", "stop:SESSION_METADATA:SOURCE_FAILURE", "start:SENSOR"),
            calls,
        )
        engine.stop(RecordingStopReason.USER_REQUEST)
    }

    @Test
    fun `Given stop failures When recording stops Then every started source is stopped`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = engine(
            FakeSource(RecordingSourceType.GPS, calls, stopFails = true),
            FakeSource(RecordingSourceType.SENSOR, calls, stopFails = true),
        )
        val request = request(
            RecordingSourceSpec.Sensors(setOf(1), 0),
            RecordingSourceSpec.Gps(10, 20),
        )
        engine.start(request)

        // When
        val result = engine.stop(RecordingStopReason.USER_REQUEST)

        // Then
        assertTrue(result.isFailure)
        assertTrue(calls.contains("stop:GPS:USER_REQUEST"))
        assertTrue(calls.contains("stop:SENSOR:USER_REQUEST"))
    }

    @Test
    fun `Given runtime and stop failures When a source fails Then both failures are published`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val session = FakeSource(RecordingSourceType.SESSION_METADATA, calls)
        val sensor = FakeSource(RecordingSourceType.SENSOR, calls, stopFails = true)
        val engine = RecordingEngine(
            sources = listOf(session, sensor),
            scope = backgroundScope,
            waitFor = { kotlinx.coroutines.delay(it) },
            sourceStartDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler),
        )
        engine.start(request(RecordingSourceSpec.SessionMetadata, RecordingSourceSpec.Sensors(setOf(1), 0)))
        val failed = async { engine.events.first { it is RecordingEvent.SourceFailed } }
        runCurrent()

        // When
        sensor.reportFailure()
        runCurrent()

        // Then
        val event = failed.await() as RecordingEvent.SourceFailed
        assertEquals(
            listOf("Record SENSOR", "Stop SENSOR"),
            listOf(event.failure.operation, event.stopFailure?.operation),
        )
        assertEquals(null, session.stopContext)
    }

    @Test
    fun `Given a completed recording When stop repeats Then sources stop once`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = engine(FakeSource(RecordingSourceType.SENSOR, calls))
        val request = request(RecordingSourceSpec.Sensors(setOf(1), 0))
        engine.start(request)

        // When
        val first = engine.stop(RecordingStopReason.USER_REQUEST)
        val second = engine.stop(RecordingStopReason.USER_REQUEST)

        // Then
        assertEquals(first, second)
        assertEquals(1, calls.count { it == "stop:SENSOR:USER_REQUEST" })
    }

    @Test
    fun `Given a recording duration When it expires Then started sources stop`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = RecordingEngine(
            sources = listOf(FakeSource(RecordingSourceType.SENSOR, calls, yieldOnStop = true)),
            scope = backgroundScope,
            waitFor = { kotlinx.coroutines.delay(it) },
            sourceStartDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler),
        )
        val request = request(RecordingSourceSpec.Sensors(setOf(1), 0), durationMillis = 500L)

        // When
        engine.start(request)
        advanceTimeBy(500L)
        runCurrent()

        // Then
        assertEquals(1, calls.count { it == "stop:SENSOR:DURATION_EXPIRED" })
    }

    @Test
    fun `Given one running source fails When another source remains Then recording continues`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val metadata = FakeSource(RecordingSourceType.SESSION_METADATA, calls)
        val source = FakeSource(RecordingSourceType.SENSOR, calls)
        val engine = RecordingEngine(
            sources = listOf(metadata, source),
            scope = backgroundScope,
            waitFor = { kotlinx.coroutines.delay(it) },
            sourceStartDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler),
        )
        val request = request(RecordingSourceSpec.SessionMetadata, RecordingSourceSpec.Sensors(setOf(1), 0))
        engine.start(request)

        // When
        source.reportFailure()
        runCurrent()

        // Then
        assertEquals(1, calls.count { it == "stop:SENSOR:SOURCE_FAILURE" })
        assertTrue(engine.stop(RecordingStopReason.USER_REQUEST).isSuccess)
        assertEquals(1, calls.count { it == "stop:SESSION_METADATA:USER_REQUEST" })
        assertEquals(0, calls.count { it == "stop:SENSOR:USER_REQUEST" })
    }

    @Test
    fun `Given every running source fails When recording stops Then metadata only session remains valid`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val sensor = FakeSource(RecordingSourceType.SENSOR, calls)
        val gps = FakeSource(RecordingSourceType.GPS, calls)
        val engine = RecordingEngine(
            sources = listOf(sensor, gps),
            scope = backgroundScope,
            waitFor = { kotlinx.coroutines.delay(it) },
            sourceStartDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler),
        )
        engine.start(
            request(
                RecordingSourceSpec.Sensors(setOf(1), 0),
                RecordingSourceSpec.Gps(10, 20),
            ),
        )

        // When
        sensor.reportFailure()
        gps.reportFailure()
        runCurrent()
        val result = engine.stop(RecordingStopReason.USER_REQUEST)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, calls.count { it == "stop:SENSOR:SOURCE_FAILURE" })
        assertEquals(1, calls.count { it == "stop:GPS:SOURCE_FAILURE" })
    }

    private fun engine(vararg sources: RecordingSource) = RecordingEngine(
        sources = sources.toList(),
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
        waitFor = { },
        sourceStartDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
    )

    private fun request(vararg sources: RecordingSourceSpec, durationMillis: Long = 0L) = RecordingRequest(
        sessionId = RecordingSessionId("session"),
        sources = sources.toList(),
        durationMillis = durationMillis,
    )
}

private class FakeSource(
    override val type: RecordingSourceType,
    private val calls: MutableList<String>,
    private val startFails: Boolean = false,
    private val stopFails: Boolean = false,
    private val hangOnStart: Boolean = false,
    private val hangOnStop: Boolean = false,
    private val yieldOnStop: Boolean = false,
) : RecordingSource {
    private val mutableFailures = MutableSharedFlow<AppError>(replay = 1)
    override val failures: Flow<AppError> = mutableFailures
    var stopContext: RecordingStopContext? = null
        private set

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> {
        calls += "start:$type"
        if (hangOnStart) awaitCancellation()
        return if (startFails) failure("Start $type") else AppResult.success(Unit)
    }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> {
        if (yieldOnStop) yield()
        stopContext = context
        calls += "stop:$type:${context.reason}"
        if (hangOnStop) awaitCancellation()
        return if (stopFails) failure("Stop $type") else AppResult.success(Unit)
    }

    fun reportFailure() {
        mutableFailures.tryEmit(AppError(AppErrorCode.RECORDING, "Record $type"))
    }

    private fun failure(operation: String): AppResult<Unit> = AppResult.failure(
        AppError(AppErrorCode.RECORDING, operation),
    )
}
