package com.tomasrepcik.sensorbox.core.diagnostics

import android.util.Log
import com.tomasrepcik.sensorbox.core.failure.DiagnosticEvent
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.failure.DiagnosticSeverity

class CompositeDiagnosticLogger(private vararg val loggers: DiagnosticLogger) : DiagnosticLogger {
    override fun record(event: DiagnosticEvent) {
        loggers.forEach { logger ->
            runCatching { logger.record(event) }
        }
    }
}

internal fun interface LogcatWriter {
    fun write(severity: DiagnosticSeverity, tag: String, message: String, cause: Throwable?)
}

class LogcatDiagnosticLogger internal constructor(private val tag: String, private val writer: LogcatWriter) :
    DiagnosticLogger {
    constructor(tag: String = "SensorBox") : this(tag, ANDROID_LOGCAT_WRITER)

    override fun record(event: DiagnosticEvent) {
        val message = "${event.code} | ${event.operation} | ${event.diagnosticMessage}"
        writer.write(event.severity, tag, message, event.cause)
    }

    private companion object {
        val ANDROID_LOGCAT_WRITER = LogcatWriter { severity, tag, message, cause ->
            when (severity) {
                DiagnosticSeverity.INFO -> Log.i(tag, message, cause)
                DiagnosticSeverity.WARNING -> Log.w(tag, message, cause)
                DiagnosticSeverity.ERROR -> Log.e(tag, message, cause)
                DiagnosticSeverity.FATAL -> Log.wtf(tag, message, cause)
            }
        }
    }
}
