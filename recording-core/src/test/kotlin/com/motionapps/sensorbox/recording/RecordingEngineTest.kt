package com.motionapps.sensorbox.recording

import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingEngineTest {
    @Test
    fun `Given unordered sources When committed Then sources start in stable order`() = runTest {
        val calls = mutableListOf<String>()
        val gps = FakeSource(RecordingSourceType.GPS, calls)
        val sensors = FakeSource(RecordingSourceType.SENSOR, calls)
        val engine = engine(listOf(gps, sensors))
        val plan = plan(
            RecordingSourceSpec.Gps(10, 20),
            RecordingSourceSpec.Sensors(setOf(1), 0),
        )

        assertTrue(engine.prepare(plan).isSuccess)
        assertTrue(engine.commit(plan.sessionId).isSuccess)

        assertEquals(
            listOf("prepare:SENSOR", "prepare:GPS", "start:SENSOR", "start:GPS"),
            calls,
        )
        assertTrue(engine.state.value is RecordingSessionState.Running)
    }

    @Test
    fun `Given a partial start failure When committed Then started sources stop in reverse order`() = runTest {
        val calls = mutableListOf<String>()
        val sensors = FakeSource(RecordingSourceType.SENSOR, calls)
        val gps = FakeSource(RecordingSourceType.GPS, calls, startFails = true)
        val engine = engine(listOf(gps, sensors))
        val plan = plan(
            RecordingSourceSpec.Sensors(setOf(1), 0),
            RecordingSourceSpec.Gps(10, 20),
        )

        engine.prepare(plan)
        val result = engine.commit(plan.sessionId)

        assertTrue(result.isFailure)
        assertEquals(listOf("stop:GPS", "stop:SENSOR"), calls.takeLast(2))
        assertEquals(RecordingSessionState.Idle, engine.state.value)
    }

    @Test
    fun `Given a prepare failure When prepared Then failing and prepared sources clean up in reverse order`() =
        runTest {
            val calls = mutableListOf<String>()
            val sensors = FakeSource(RecordingSourceType.SENSOR, calls)
            val gps = FakeSource(RecordingSourceType.GPS, calls, prepareFails = true)
            val engine = engine(listOf(gps, sensors))
            val plan = plan(
                RecordingSourceSpec.Sensors(setOf(1), 0),
                RecordingSourceSpec.Gps(10, 20),
            )

            val result = engine.prepare(plan)

            assertTrue(result.isFailure)
            assertEquals(listOf("stop:GPS", "stop:SENSOR"), calls.takeLast(2))
            assertEquals(RecordingSessionState.Idle, engine.state.value)
        }

    @Test
    fun `Given stop failures When stopped Then every source is attempted`() = runTest {
        val calls = mutableListOf<String>()
        val sensors = FakeSource(RecordingSourceType.SENSOR, calls, stopFails = true)
        val gps = FakeSource(RecordingSourceType.GPS, calls, stopFails = true)
        val engine = engine(listOf(gps, sensors))
        val plan = plan(
            RecordingSourceSpec.Sensors(setOf(1), 0),
            RecordingSourceSpec.Gps(10, 20),
        )
        engine.prepare(plan)
        engine.commit(plan.sessionId)

        val result = engine.stop(plan.sessionId, RecordingStopReason.USER_REQUEST)

        assertTrue(result.isFailure)
        assertTrue(calls.contains("stop:GPS"))
        assertTrue(calls.contains("stop:SENSOR"))
    }

    @Test
    fun `Given a completed session When stop repeats Then sources are not stopped twice`() = runTest {
        val calls = mutableListOf<String>()
        val sensors = FakeSource(RecordingSourceType.SENSOR, calls)
        val engine = engine(listOf(sensors))
        val plan = plan(RecordingSourceSpec.Sensors(setOf(1), 0))
        engine.prepare(plan)
        engine.commit(plan.sessionId)

        val first = engine.stop(plan.sessionId, RecordingStopReason.USER_REQUEST)
        val second = engine.stop(plan.sessionId, RecordingStopReason.USER_REQUEST)

        assertEquals(first, second)
        assertEquals(1, calls.count { it == "stop:SENSOR" })
    }

    @Test
    fun `Given a duration When committed Then recording stops after duration`() = runTest {
        val calls = mutableListOf<String>()
        val sensors = FakeSource(RecordingSourceType.SENSOR, calls)
        val engine = RecordingEngine(
            sources = listOf(sensors),
            scope = backgroundScope,
            clock = RecordingClock { testScheduler.currentTime },
            delay = RecordingDelay { kotlinx.coroutines.delay(it) },
        )
        val plan = plan(RecordingSourceSpec.Sensors(setOf(1), 0), durationMillis = 500L)

        engine.prepare(plan)
        engine.commit(plan.sessionId)
        advanceTimeBy(500L)
        runCurrent()

        assertEquals(RecordingSessionState.Idle, engine.state.value)
        assertEquals(1, calls.count { it == "stop:SENSOR" })
    }

    @Test
    fun `Given a running session When low battery stops it Then terminal state returns to idle`() = runTest {
        val calls = mutableListOf<String>()
        val sensors = FakeSource(RecordingSourceType.SENSOR, calls)
        val engine = engine(listOf(sensors))
        val plan = plan(RecordingSourceSpec.Sensors(setOf(1), 0))
        engine.prepare(plan)
        engine.commit(plan.sessionId)

        val result = engine.stop(plan.sessionId, RecordingStopReason.LOW_BATTERY)

        assertTrue(result.isSuccess)
        assertEquals(RecordingSessionState.Idle, engine.state.value)
        assertEquals(1, calls.count { it == "stop:SENSOR" })
    }

    private fun engine(sources: List<RecordingSource>) = RecordingEngine(
        sources = sources,
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
        clock = RecordingClock { 0L },
        delay = RecordingDelay { },
    )

    private fun plan(vararg sources: RecordingSourceSpec, durationMillis: Long = 0L) = RecordingPlan(
        sessionId = RecordingSessionId.create(),
        sources = sources.toList(),
        startAtEpochMillis = 0L,
        durationMillis = durationMillis,
    )
}

private class FakeSource(
    override val type: RecordingSourceType,
    private val calls: MutableList<String>,
    private val prepareFails: Boolean = false,
    private val startFails: Boolean = false,
    private val stopFails: Boolean = false,
) : RecordingSource {
    override suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit> {
        calls += "prepare:$type"
        return if (prepareFails) failure("Prepare $type") else AppResult.success(Unit)
    }

    override suspend fun start(): AppResult<Unit> {
        calls += "start:$type"
        return if (startFails) failure("Start $type") else AppResult.success(Unit)
    }

    override suspend fun stop(): AppResult<Unit> {
        calls += "stop:$type"
        return if (stopFails) failure("Stop $type") else AppResult.success(Unit)
    }

    private fun failure(operation: String): AppResult<Unit> = AppResult.failure(
        AppError(AppErrorCode.MEASUREMENT, operation),
    )
}
