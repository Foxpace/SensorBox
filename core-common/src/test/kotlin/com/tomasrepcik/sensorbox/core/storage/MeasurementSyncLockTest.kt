package com.tomasrepcik.sensorbox.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementSyncLockTest {
    @Test
    fun `Given sync is active When recording or archive changes are attempted Then neither operation runs`() {
        // Given
        val lock = MeasurementSyncLock()
        lock.begin("request")
        var calls = 0

        // When
        val result = runCatching { lock.whenIdle { calls += 1 } }

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, calls)
    }

    @Test
    fun `Given a retry is active When the old request finishes Then recording remains blocked`() {
        // Given
        val lock = MeasurementSyncLock()
        lock.begin("old")
        lock.begin("retry")

        // When
        lock.finish("old")

        // Then
        assertTrue(lock.busy.value)
        assertTrue(runCatching { lock.whenIdle { } }.isFailure)
        lock.finish("retry")
        assertFalse(lock.busy.value)
    }

    @Test
    fun `Given a cancelled request When commit is attempted Then its archive operation cannot run`() {
        // Given
        val lock = MeasurementSyncLock()
        lock.begin("request")
        lock.finish("request")

        // When
        val result = runCatching { lock.whileActive("request") { error("Must not run") } }

        // Then
        assertEquals("Watch sync is no longer active", result.exceptionOrNull()?.message)
    }
}
