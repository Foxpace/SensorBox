package com.motionapps.sensorbox.core.error

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticMetadata(
    val appVersion: String,
    val buildType: String,
    val deviceModel: String,
    val androidVersion: String,
    val processName: String,
)

class FileDiagnostics internal constructor(
    private val diagnosticsDirectory: File,
    private val metadata: DiagnosticMetadata,
) : DiagnosticLogger,
    DiagnosticsStore {
    private val lock = Any()

    constructor(context: Context, metadata: DiagnosticMetadata) : this(
        diagnosticsDirectory = File(context.filesDir, DIRECTORY_NAME),
        metadata = metadata,
    )

    @Volatile
    private var uncaughtHandlerInstalled = false

    override fun record(event: DiagnosticEvent) {
        val entry = event.toDiagnosticEntry().take(MAX_ENTRY_CHARS)
        synchronized(lock) {
            appendSafely(entry)
        }
    }

    fun installUncaughtExceptionHandler() {
        if (uncaughtHandlerInstalled) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            record(
                DiagnosticEvent(
                    severity = DiagnosticSeverity.FATAL,
                    code = AppErrorCode.UNKNOWN,
                    operation = "Uncaught exception",
                    diagnosticMessage = "Uncaught ${error::class.java.simpleName}",
                    cause = error,
                    context = mapOf("threadName" to thread.name),
                ),
            )
            previous?.uncaughtException(thread, error)
        }
        uncaughtHandlerInstalled = true
    }

    override fun readText(): AppResult<String> = appResult(AppErrorCode.STORAGE, "Read diagnostics") {
        synchronized(lock) {
            val files = listOf(previousFile(), currentFile()).filter(File::exists)
            if (files.isEmpty()) NO_DIAGNOSTICS else files.joinToString(separator = "") { it.readText() }
        }
    }

    override fun exportFile(): AppResult<File> = readText().flatMap { text ->
        appResult(AppErrorCode.STORAGE, "Export diagnostics") {
            synchronized(lock) {
                diagnosticsDirectory.mkdirs()
                exportFilePath().apply { writeText(text) }
            }
        }
    }

    override fun clear(): AppResult<Unit> = appResult(AppErrorCode.STORAGE, "Clear diagnostics") {
        synchronized(lock) {
            listOf(currentFile(), previousFile(), exportFilePath()).forEach { file ->
                check(!file.exists() || file.delete()) { "Unable to delete diagnostics" }
            }
        }
    }

    private fun appendSafely(entry: String) {
        try {
            diagnosticsDirectory.mkdirs()
            val file = currentFile()
            if (file.length() + entry.toByteArray().size > MAX_FILE_BYTES) rotate(file)
            file.appendText(entry)
        } catch (_: Throwable) {
            // A diagnostics write must never become an application failure.
        }
    }

    private fun rotate(file: File) {
        val previous = previousFile()
        if (previous.exists()) previous.delete()
        if (file.exists()) file.renameTo(previous)
    }

    private fun DiagnosticEvent.toDiagnosticEntry(): String = buildString {
        val timestamp = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date())
        append(timestamp).append(" | ").append(severity).append(" | ").append(code).append(" | ")
            .append(operation.safeText()).appendLine()
        append("message=").append(diagnosticMessage.safeText()).appendLine()
        append("appVersion=").append(metadata.appVersion.safeText()).appendLine()
        append("buildType=").append(metadata.buildType.safeText()).appendLine()
        append("deviceModel=").append(metadata.deviceModel.safeText()).appendLine()
        append("androidVersion=").append(metadata.androidVersion.safeText()).appendLine()
        append("process=").append(metadata.processName.safeText()).appendLine()
        context.filterKeys(SAFE_CONTEXT_KEYS::contains).toSortedMap().forEach { (key, value) ->
            append(key).append('=').append(value.safeText()).appendLine()
        }
        cause?.let { error ->
            append("exception=").append(error::class.java.name).appendLine()
            error.stackTrace.take(MAX_STACK_FRAMES).forEach { frame ->
                append("at ").append(frame.className).append('.').append(frame.methodName)
                    .append('(').append(frame.fileName?.substringAfterLast('/')?.safeText() ?: "Unknown")
                    .append(':').append(frame.lineNumber).appendLine(")")
            }
        }
        appendLine(ENTRY_SEPARATOR)
    }

    private fun String.safeText(): String = replace('\n', ' ').replace('\r', ' ').take(MAX_FIELD_CHARS)

    private fun currentFile(): File = File(diagnosticsDirectory, FILE_NAME)

    private fun previousFile(): File = File(diagnosticsDirectory, PREVIOUS_FILE_NAME)

    private fun exportFilePath(): File = File(diagnosticsDirectory, EXPORT_FILE_NAME)

    private companion object {
        const val DIRECTORY_NAME = "diagnostics"
        const val FILE_NAME = "sensorbox-diagnostics.txt"
        const val PREVIOUS_FILE_NAME = "sensorbox-diagnostics-previous.txt"
        const val EXPORT_FILE_NAME = "sensorbox-diagnostics-export.txt"
        const val TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        const val ENTRY_SEPARATOR = "---"
        const val NO_DIAGNOSTICS = "No diagnostics have been recorded.\n"
        const val MAX_ENTRY_CHARS = 32_000
        const val MAX_FIELD_CHARS = 512
        const val MAX_STACK_FRAMES = 80
        const val MAX_FILE_BYTES = 1_000_000L
        val SAFE_CONTEXT_KEYS = setOf(
            "androidVersion",
            "buildType",
            "durationMillis",
            "failureCount",
            "failedOperation",
            "processName",
            "protocolVersion",
            "retryCount",
            "samplingPeriod",
            "sessionId",
            "sourceCount",
            "sourceType",
            "state",
            "stopReason",
            "threadName",
        )
    }
}
