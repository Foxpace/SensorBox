package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
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
        assertEquals(listOf("stop:GPS", "stop:SENSOR"), calls.takeLast(2))
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
        val result = engine.stop(plan.sessionId, RecordingStopReason.USER_REQUEST)

        // Then
        assertTrue(result.isFailure)
        assertTrue(calls.contains("stop:GPS"))
        assertTrue(calls.contains("stop:SENSOR"))
    }

    @Test
    fun `Given a completed recording When stop repeats Then sources stop once`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = engine(FakeSource(RecordingSourceType.SENSOR, calls))
        val plan = plan(RecordingSourceSpec.Sensors(setOf(1), 0))
        engine.start(plan)

        // When
        val first = engine.stop(plan.sessionId, RecordingStopReason.USER_REQUEST)
        val second = engine.stop(plan.sessionId, RecordingStopReason.USER_REQUEST)

        // Then
        assertEquals(first, second)
        assertEquals(1, calls.count { it == "stop:SENSOR" })
    }

    @Test
    fun `Given a recording duration When it expires Then started sources stop`() = runTest {
        // Given
        val calls = mutableListOf<String>()
        val engine = RecordingEngine(
            sources = listOf(FakeSource(RecordingSourceType.SENSOR, calls)),
            scope = backgroundScope,
            clock = RecordingClock { testScheduler.currentTime },
            delay = RecordingDelay { kotlinx.coroutines.delay(it) },
        )
        val plan = plan(RecordingSourceSpec.Sensors(setOf(1), 0), durationMillis = 500L)

        // When
        engine.start(plan)
        advanceTimeBy(500L)
        runCurrent()

        // Then
        assertEquals(1, calls.count { it == "stop:SENSOR" })
    }

    private fun engine(vararg sources: RecordingSource) = RecordingEngine(
        sources = sources.toList(),
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
        clock = RecordingClock { 0L },
        delay = RecordingDelay { },
    )

    private fun plan(vararg sources: RecordingSourceSpec, durationMillis: Long = 0L) = RecordingPlan(
        sessionId = RecordingSessionId.create(),
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
    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> {
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
