package com.tomasrepcik.sensorbox.core.error

import org.junit.Assert.assertEquals
import org.junit.Test

class CompositeDiagnosticLoggerTest {
    @Test
    fun `Given one failed logger When an event is recorded Then remaining loggers still receive it`() {
        val received = mutableListOf<DiagnosticEvent>()
        val logger = CompositeDiagnosticLogger(
            DiagnosticLogger { error("disk failed") },
            DiagnosticLogger(received::add),
        )

        logger.record(event(DiagnosticSeverity.ERROR))

        assertEquals(listOf(DiagnosticSeverity.ERROR), received.map(DiagnosticEvent::severity))
    }

    @Test
    fun `Given every severity When Logcat records Then severity tag message and cause are routed`() {
        val writes = mutableListOf<LogcatWrite>()
        val cause = IllegalStateException("fixture")
        val logger = LogcatDiagnosticLogger("TestTag") { severity, tag, message, error ->
            writes += LogcatWrite(severity, tag, message, error)
        }

        DiagnosticSeverity.entries.forEach { severity -> logger.record(event(severity, cause)) }

        assertEquals(DiagnosticSeverity.entries, writes.map(LogcatWrite::severity))
        assertEquals(setOf("TestTag"), writes.map(LogcatWrite::tag).toSet())
        assertEquals(setOf(cause), writes.map(LogcatWrite::cause).toSet())
        assertEquals(true, writes.all { "STORAGE | Test operation | Test message" == it.message })
    }

    private fun event(severity: DiagnosticSeverity, cause: Throwable? = null) = DiagnosticEvent(
        severity = severity,
        code = AppErrorCode.STORAGE,
        operation = "Test operation",
        diagnosticMessage = "Test message",
        cause = cause,
    )

    private data class LogcatWrite(
        val severity: DiagnosticSeverity,
        val tag: String,
        val message: String,
        val cause: Throwable?,
    )
}
