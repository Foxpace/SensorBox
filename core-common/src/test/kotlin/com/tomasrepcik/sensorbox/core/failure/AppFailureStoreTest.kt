package com.tomasrepcik.sensorbox.core.failure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppFailureStoreTest {
    @Test
    fun `Given no visible failure When one is shown Then it becomes visible and is recorded`() {
        // Given
        val fixture = fixture()
        val failure = AppError(AppErrorCode.STORAGE, "Load archive")

        // When
        fixture.store.show(failure)

        // Then
        assertEquals(failure, fixture.store.visibleFailure.value)
        assertEquals(1, fixture.events.size)
    }

    @Test
    fun `Given a visible failure When another is shown Then first remains visible and both are recorded`() {
        // Given
        val fixture = fixture()
        val first = AppError(AppErrorCode.STORAGE, "Load archive")
        val second = AppError(AppErrorCode.CONNECTIVITY, "Connect watch")
        fixture.store.show(first)

        // When
        fixture.store.show(second)

        // Then
        assertEquals(first, fixture.store.visibleFailure.value)
        assertEquals(2, fixture.events.size)
    }

    @Test
    fun `Given a visible failure When dismissed Then no failure remains visible`() {
        // Given
        val fixture = fixture()
        fixture.store.show(AppError(AppErrorCode.UNKNOWN, "Run operation"))

        // When
        fixture.store.dismiss()

        // Then
        assertNull(fixture.store.visibleFailure.value)
    }

    @Test
    fun `Given a recording source failure When recorded only Then diagnostics receive it without visible failure`() {
        // Given
        val fixture = fixture()

        // When
        fixture.store.recordOnly(AppError(AppErrorCode.RECORDING, "Write accelerometer"))

        // Then
        assertNull(fixture.store.visibleFailure.value)
        assertEquals(1, fixture.events.size)
    }

    private fun fixture(): Fixture {
        val events = mutableListOf<DiagnosticEvent>()
        return Fixture(AppFailureStore(events::add), events)
    }

    private data class Fixture(val store: AppFailureStore, val events: MutableList<DiagnosticEvent>)
}
