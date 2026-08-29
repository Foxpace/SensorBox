package com.tomasrepcik.sensorbox.core.diagnostics

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.DiagnosticEvent
import com.tomasrepcik.sensorbox.core.failure.DiagnosticSeverity
import com.tomasrepcik.sensorbox.core.time.EpochClock
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class FileDiagnosticsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `Given eight calendar days When events are recorded Then only seven dated files remain`() {
        val directory = temporaryFolder.newFolder("diagnostics")
        var currentMillis = Instant.parse("2026-08-01T12:00:00Z").toEpochMilliseconds()
        val diagnostics = diagnostics(directory, EpochClock { currentMillis })

        repeat(8) { day ->
            diagnostics.record(event(message = "day $day"))
            currentMillis += 24 * 60 * 60 * 1_000L
        }

        val retainedLogs = directory.listFiles().orEmpty().filter { "export" !in it.name }
        assertEquals(7, retainedLogs.size)
        assertFalse(retainedLogs.any { it.name.contains("2026-08-01") })
        assertTrue(diagnostics.readText().getOrNull().orEmpty().contains("day 7"))
    }

    @Test
    fun `Given private context When event is recorded Then only allowlisted context is written`() {
        val diagnostics = diagnostics(temporaryFolder.newFolder("diagnostics"))
        diagnostics.record(
            event(
                message = "Controlled failure message",
                context = mapOf(
                    "sessionId" to "session-123",
                    "sensorSamples" to "1.2,3.4,5.6",
                    "coordinates" to "48.1,17.1",
                    "annotation" to "private note",
                    "recordingName" to "private recording",
                    "safUri" to "content://private/path",
                    "rawPayload" to "secret bytes",
                    "deviceId" to "unique-device-id",
                ),
            ),
        )

        val text = diagnostics.readText().getOrNull().orEmpty()
        assertTrue(text.contains("\"sessionId\":\"session-123\""))
        assertFalse(text.contains("1.2,3.4,5.6"))
        assertFalse(text.contains("48.1,17.1"))
        assertFalse(text.contains("private note"))
        assertFalse(text.contains("private recording"))
        assertFalse(text.contains("content://private/path"))
        assertFalse(text.contains("secret bytes"))
        assertFalse(text.contains("unique-device-id"))
    }

    @Test
    fun `Given an unwritable location When event is recorded Then logger does not throw`() {
        val fileInsteadOfDirectory = temporaryFolder.newFile("not-a-directory")
        val diagnostics = diagnostics(fileInsteadOfDirectory)

        diagnostics.record(event(message = "Write failure"))

        assertTrue(fileInsteadOfDirectory.isFile)
    }

    @Test
    fun `Given retained logs When cleared Then reading returns empty text`() {
        val diagnostics = diagnostics(temporaryFolder.newFolder("diagnostics"))
        diagnostics.record(event(message = "Before clear"))

        assertTrue(diagnostics.clear().isSuccess)

        assertEquals("", diagnostics.readText().getOrNull())
    }

    @Test
    fun `Given concurrent installation When an exception is dispatched Then the handler is installed once`() {
        val diagnostics = diagnostics(temporaryFolder.newFolder("diagnostics"))
        val original = Thread.getDefaultUncaughtExceptionHandler()
        val delegated = AtomicInteger()
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> delegated.incrementAndGet() }
        try {
            List(20) { thread(start = true) { diagnostics.installUncaughtExceptionHandler() } }
                .forEach(Thread::join)

            Thread.getDefaultUncaughtExceptionHandler()
                ?.uncaughtException(Thread.currentThread(), IllegalStateException("fixture"))

            val text = diagnostics.readText().getOrNull().orEmpty()
            assertEquals(1, text.split("Uncaught IllegalStateException").size - 1)
            assertEquals(1, delegated.get())
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(original)
        }
    }

    private fun diagnostics(
        directory: java.io.File,
        clock: EpochClock = EpochClock { Instant.parse("2026-08-23T12:00:00Z").toEpochMilliseconds() },
    ) = FileDiagnostics(
        diagnosticsDirectory = directory,
        metadata = DiagnosticMetadata(
            appVersion = "1.0",
            buildType = "test",
            deviceModel = "test model",
            androidVersion = "test android",
            processName = "test process",
        ),
        clock = clock,
    )

    private fun event(message: String, context: Map<String, String> = emptyMap()) = DiagnosticEvent(
        severity = DiagnosticSeverity.ERROR,
        code = AppErrorCode.STORAGE,
        operation = "Test operation",
        diagnosticMessage = message,
        context = context,
    )
}
