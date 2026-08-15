package com.motionapps.sensorbox.core.error

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppDiagnostics {
    private val lock = Any()
    private val pending = ArrayDeque<String>()

    @Volatile
    private var applicationContext: Context? = null

    @Volatile
    private var uncaughtHandlerInstalled = false

    fun install(context: Context) {
        synchronized(lock) {
            applicationContext = context.applicationContext
            installUncaughtExceptionHandler()
            val queued = pending.toList()
            pending.clear()
            queued.forEach(::appendSafely)
        }
    }

    private fun installUncaughtExceptionHandler() {
        if (uncaughtHandlerInstalled) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            AppError.from(AppError.Kind.UNKNOWN, "Uncaught exception on ${thread.name}", error)
            previous?.uncaughtException(thread, error)
        }
        uncaughtHandlerInstalled = true
    }

    fun record(error: AppError) {
        val entry = error.toDiagnosticEntry()
        synchronized(lock) {
            if (applicationContext == null) {
                if (pending.size == MAX_PENDING_ENTRIES) pending.removeFirst()
                pending.addLast(entry)
            } else {
                appendSafely(entry)
            }
        }
    }

    fun readText(): Result<String> = appResult(AppError.Kind.STORAGE, "Read diagnostics") {
        synchronized(lock) {
            val file = diagnosticsFile()
            if (file.exists()) file.readText() else NO_DIAGNOSTICS
        }
    }

    fun exportFile(): Result<File> = appResult(AppError.Kind.STORAGE, "Export diagnostics") {
        synchronized(lock) {
            diagnosticsFile().also { file ->
                file.parentFile?.mkdirs()
                if (!file.exists()) file.writeText(NO_DIAGNOSTICS)
            }
        }
    }

    fun clear(): Result<Unit> = appResult(AppError.Kind.STORAGE, "Clear diagnostics") {
        synchronized(lock) {
            val file = diagnosticsFile()
            check(!file.exists() || file.delete()) { "Unable to delete diagnostics" }
        }
    }

    private fun appendSafely(entry: String) {
        try {
            val file = diagnosticsFile()
            file.parentFile?.mkdirs()
            if (file.length() + entry.length > MAX_FILE_BYTES) rotate(file)
            file.appendText(entry)
        } catch (_: Throwable) {
            // Diagnostics must never become a second failure source.
        }
    }

    private fun rotate(file: File) {
        val previous = File(file.parentFile, PREVIOUS_FILE_NAME)
        if (previous.exists()) previous.delete()
        if (file.exists()) file.renameTo(previous)
    }

    private fun diagnosticsFile(): File {
        val context = checkNotNull(applicationContext) { "Diagnostics are not initialized" }
        return File(File(context.filesDir, DIRECTORY_NAME), FILE_NAME)
    }

    private fun AppError.toDiagnosticEntry(): String = buildString {
        val timestamp = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date())
        append(timestamp).append(" | ").append(kind).append(" | ").append(operation).appendLine()
        append(stackTraceToString().take(MAX_ENTRY_CHARS)).appendLine()
        appendLine(ENTRY_SEPARATOR)
    }

    private const val DIRECTORY_NAME = "diagnostics"
    private const val FILE_NAME = "sensorbox-diagnostics.txt"
    private const val PREVIOUS_FILE_NAME = "sensorbox-diagnostics-previous.txt"
    private const val TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    private const val ENTRY_SEPARATOR = "---"
    private const val NO_DIAGNOSTICS = "No diagnostics have been recorded.\n"
    private const val MAX_PENDING_ENTRIES = 20
    private const val MAX_ENTRY_CHARS = 32_000
    private const val MAX_FILE_BYTES = 1_000_000L
}
