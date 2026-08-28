package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
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
        val engine = engine(
            FakeSource(RecordingSourceType.GPS, calls),
            FakeSource(RecordingSourceType.SENSOR, calls),
        )
        val plan = plan(
            RecordingSourceSpec.Gps(10, 20),
            RecordingSourceSpec.Sensors(setOf(1), 0),
        )

        // When
        val result = engine.start(plan)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("start:SENSOR", "start:GPS"), calls)
    }

    @Test
    fun `Given a source start fails When recording starts Then attempted sources stop in reverse order`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = engine(
            FakeSource(RecordingSourceType.GPS, calls, startFails = true),
            FakeSource(RecordingSourceType.SENSOR, calls),
        )
        val plan = plan(
            RecordingSourceSpec.Sensors(setOf(1), 0),
            RecordingSourceSpec.Gps(10, 20),
        )

        // When
        val result = engine.start(plan)

        // Then
        assertTrue(result.isFailure)
        assertEquals(
            listOf("stop:GPS:SOURCE_FAILURE", "stop:SENSOR:SOURCE_FAILURE"),
            calls.takeLast(2),
        )
    }

    @Test
    fun `Given stop failures When recording stops Then every started source is stopped`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = engine(
            FakeSource(RecordingSourceType.GPS, calls, stopFails = true),
            FakeSource(RecordingSourceType.SENSOR, calls, stopFails = true),
        )
        val plan = plan(
            RecordingSourceSpec.Sensors(setOf(1), 0),
            RecordingSourceSpec.Gps(10, 20),
        )
        engine.start(plan)

        // When
        val result = engine.stop(RecordingStopReason.USER_REQUEST)

        // Then
        assertTrue(result.isFailure)
        assertTrue(calls.contains("stop:GPS:USER_REQUEST"))
        assertTrue(calls.contains("stop:SENSOR:USER_REQUEST"))
    }

    @Test
    fun `Given runtime and stop failures When session stops Then session receives every failure`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val session = FakeSource(RecordingSourceType.SESSION, calls)
        val sensor = FakeSource(RecordingSourceType.SENSOR, calls, stopFails = true)
        val engine = RecordingEngine(
            sources = listOf(session, sensor),
            scope = backgroundScope,
            waitFor = { kotlinx.coroutines.delay(it) },
        )
        engine.start(plan(RecordingSourceSpec.Session, RecordingSourceSpec.Sensors(setOf(1), 0)))

        // When
        sensor.reportFailure()
        runCurrent()

        // Then
        assertEquals(
            listOf("Record SENSOR", "Stop SENSOR"),
            session.stopContext?.failures?.map(AppError::operation),
        )
    }

    @Test
    fun `Given a completed recording When stop repeats Then sources stop once`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = engine(FakeSource(RecordingSourceType.SENSOR, calls))
        val plan = plan(RecordingSourceSpec.Sensors(setOf(1), 0))
        engine.start(plan)

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
            sources = listOf(FakeSource(RecordingSourceType.SENSOR, calls)),
            scope = backgroundScope,
            waitFor = { kotlinx.coroutines.delay(it) },
        )
        val plan = plan(RecordingSourceSpec.Sensors(setOf(1), 0), durationMillis = 500L)

        // When
        engine.start(plan)
        advanceTimeBy(500L)
        runCurrent()

        // Then
        assertEquals(1, calls.count { it == "stop:SENSOR:DURATION_EXPIRED" })
    }

    @Test
    fun `Given a running source When it reports failure Then the recording stops`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val source = FakeSource(RecordingSourceType.SENSOR, calls)
        val engine = RecordingEngine(
            sources = listOf(source),
            scope = backgroundScope,
            waitFor = { kotlinx.coroutines.delay(it) },
        )
        val plan = plan(RecordingSourceSpec.Sensors(setOf(1), 0))
        engine.start(plan)

        // When
        source.reportFailure()
        runCurrent()

        // Then
        assertEquals(1, calls.count { it == "stop:SENSOR:SOURCE_FAILURE" })
        assertTrue(engine.stop(RecordingStopReason.USER_REQUEST).isFailure)
    }

    private fun engine(vararg sources: RecordingSource) = RecordingEngine(
        sources = sources.toList(),
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
        waitFor = { },
    )

    private fun plan(vararg sources: RecordingSourceSpec, durationMillis: Long = 0L) = RecordingPlan(
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
) : RecordingSource {
    private val mutableFailures = MutableSharedFlow<AppError>(replay = 1)
    override val failures: Flow<AppError> = mutableFailures
    var stopContext: RecordingStopContext? = null
        private set

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> {
        calls += "start:$type"
        return if (startFails) failure("Start $type") else AppResult.success(Unit)
    }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> {
        stopContext = context
        calls += "stop:$type:${context.reason}"
        return if (stopFails) failure("Stop $type") else AppResult.success(Unit)
    }

    fun reportFailure() {
        mutableFailures.tryEmit(AppError(AppErrorCode.MEASUREMENT, "Record $type"))
    }

    private fun failure(operation: String): AppResult<Unit> = AppResult.failure(
        AppError(AppErrorCode.MEASUREMENT, operation),
    )
}
