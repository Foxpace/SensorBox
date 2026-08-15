package com.motionapps.sensorbox.core.error

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorTest {
    @Test
    fun `Given an operation failure When captured Then AppError is returned`() {
        val cause = IllegalStateException("disk unavailable")

        val result = appResult(AppError.Kind.STORAGE, "Write file") { throw cause }

        val error = result.exceptionOrNull()
        assertTrue(error is AppError)
        assertEquals(AppError.Kind.STORAGE, (error as AppError).kind)
        assertEquals("Write file", error.operation)
        assertSame(cause, error.cause)
    }

    @Test(expected = CancellationException::class)
    fun `Given coroutine cancellation When captured Then cancellation is rethrown`() {
        runBlocking {
            suspendAppResult<Unit>(AppError.Kind.CONNECTIVITY, "Send message") {
                throw CancellationException("cancelled")
            }
        }
    }
}
