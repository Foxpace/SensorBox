package com.tomasrepcik.sensorbox.core.diagnostics

import android.content.Context
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.DiagnosticEvent
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.failure.DiagnosticSeverity
import com.tomasrepcik.sensorbox.core.failure.DiagnosticsStore
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.format.ClockFormats
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.core.time.SystemEpochClock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Instant

@Serializable
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
    private val clock: EpochClock = SystemEpochClock,
) : DiagnosticLogger,
    DiagnosticsStore {
    private val fileLock = Any()
    private val handlerLock = Any()

    constructor(context: Context, metadata: DiagnosticMetadata) : this(
        diagnosticsDirectory = File(context.filesDir, DIRECTORY_NAME),
        metadata = metadata,
    )

    private var uncaughtHandlerInstalled = false

    override fun record(event: DiagnosticEvent) {
        val entry = JSON.encodeToString(event.toStoredEntry()) + "\n"
        synchronized(fileLock) {
            appendSafely(entry)
        }
    }

    fun installUncaughtExceptionHandler() {
        synchronized(handlerLock) {
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
    }

    override fun readText(): AppResult<String> = appResult(AppErrorCode.STORAGE, "Read diagnostics") {
        synchronized(fileLock) {
            readTextLocked()
        }
    }

    override fun clear(): AppResult<Unit> = appResult(AppErrorCode.STORAGE, "Clear diagnostics") {
        synchronized(fileLock) {
            diagnosticFiles().plus(exportFilePath()).forEach { file ->
                check(!file.exists() || file.delete()) { "Unable to delete diagnostics" }
            }
        }
    }

    private fun appendSafely(entry: String) {
        try {
            diagnosticsDirectory.mkdirs()
            pruneStaleFiles()
            currentFile().appendText(entry)
        } catch (_: Throwable) {
            // A diagnostics write must never become an application failure.
        }
    }

    private fun DiagnosticEvent.toStoredEntry() = StoredDiagnosticEntry(
        timestamp = ClockFormats.diagnosticTimestamp(clock.nowMillis()),
        severity = severity.name,
        code = code.name,
        operation = operation.safeText(),
        message = diagnosticMessage.safeText(),
        metadata = metadata,
        context = context.filterKeys(SAFE_CONTEXT_KEYS::contains)
            .mapValues { (_, value) -> value.safeText() }
            .toSortedMap(),
        exception = cause?.javaClass?.name,
        stackTrace = cause?.stackTrace.orEmpty().take(MAX_STACK_FRAMES).map { frame ->
            "${frame.className}.${frame.methodName}(" +
                "${frame.fileName?.substringAfterLast('/')?.safeText() ?: "Unknown"}:${frame.lineNumber})"
        },
    )

    private fun String.safeText(): String = replace('\n', ' ').replace('\r', ' ').take(MAX_FIELD_CHARS)

    private fun readTextLocked(): String {
        val files = diagnosticFiles().sortedBy(File::getName)
        return files.joinToString(separator = "") { it.readText() }
    }

    private fun diagnosticFiles(): List<File> = diagnosticsDirectory.listFiles().orEmpty()
        .filter {
            it.isFile &&
                it.name != EXPORT_FILE_NAME &&
                it.name.startsWith(FILE_PREFIX) &&
                it.name.endsWith(FILE_SUFFIX)
        }

    private fun currentFile(): File = File(
        diagnosticsDirectory,
        "$FILE_PREFIX${currentDate()}$FILE_SUFFIX",
    )

    private fun pruneStaleFiles() {
        val retainedDates = (0 until RETENTION_DAYS).map { days ->
            currentDate().minus(
                DatePeriod(days = days),
            )
        }.toSet()
        diagnosticFiles().forEach { file ->
            val dateText = file.name.removePrefix(FILE_PREFIX).removeSuffix(FILE_SUFFIX)
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
            if (date == null || date !in retainedDates) file.delete()
        }
    }

    private fun currentDate(): LocalDate = Instant.fromEpochMilliseconds(clock.nowMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    private fun exportFilePath(): File = File(diagnosticsDirectory, EXPORT_FILE_NAME)

    private companion object {
        const val DIRECTORY_NAME = "diagnostics"
        const val FILE_PREFIX = "sensorbox-diagnostics-"
        const val FILE_SUFFIX = ".jsonl"
        const val EXPORT_FILE_NAME = "sensorbox-diagnostics-export.jsonl"
        const val MAX_FIELD_CHARS = 512
        const val MAX_STACK_FRAMES = 80
        const val RETENTION_DAYS = 7
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
        val JSON = Json { encodeDefaults = true }
    }
}

@Serializable
private data class StoredDiagnosticEntry(
    val timestamp: String,
    val severity: String,
    val code: String,
    val operation: String,
    val message: String,
    val metadata: DiagnosticMetadata,
    val context: Map<String, String>,
    val exception: String? = null,
    val stackTrace: List<String> = emptyList(),
)
