package com.motionapps.sensorbox.core.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileDiagnosticsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `Given many events When logs rotate Then only two one megabyte files remain`() {
        val directory = temporaryFolder.newFolder("diagnostics")
        val diagnostics = diagnostics(directory)

        repeat(2_500) { index -> diagnostics.record(event(message = "$index ${"x".repeat(1_000)}")) }

        val retainedLogs = directory.listFiles().orEmpty().filter { "export" !in it.name }
        assertEquals(2, retainedLogs.size)
        assertTrue(retainedLogs.all { it.length() <= 1_000_000L })
        assertTrue(diagnostics.readText().getOrNull().orEmpty().contains("2499 "))
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
        assertTrue(text.contains("sessionId=session-123"))
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
    fun `Given retained logs When cleared Then reading returns the empty message`() {
        val diagnostics = diagnostics(temporaryFolder.newFolder("diagnostics"))
        diagnostics.record(event(message = "Before clear"))

        assertTrue(diagnostics.clear().isSuccess)

        assertEquals("No diagnostics have been recorded.\n", diagnostics.readText().getOrNull())
    }

    private fun diagnostics(directory: java.io.File) = FileDiagnostics(
        diagnosticsDirectory = directory,
        metadata = DiagnosticMetadata(
            appVersion = "1.0",
            buildType = "test",
            deviceModel = "test model",
            androidVersion = "test android",
            processName = "test process",
        ),
    )

    private fun event(message: String, context: Map<String, String> = emptyMap()) = DiagnosticEvent(
        severity = DiagnosticSeverity.ERROR,
        code = AppErrorCode.STORAGE,
        operation = "Test operation",
        diagnosticMessage = message,
        context = context,
    )
}
