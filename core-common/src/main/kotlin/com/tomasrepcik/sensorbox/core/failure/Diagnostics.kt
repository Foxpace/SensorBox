package com.tomasrepcik.sensorbox.core.failure

enum class DiagnosticSeverity {
    INFO,
    WARNING,
    ERROR,
    FATAL,
}

data class DiagnosticEvent(
    val severity: DiagnosticSeverity,
    val code: AppErrorCode,
    val operation: String,
    val diagnosticMessage: String,
    val cause: Throwable? = null,
    val context: Map<String, String> = emptyMap(),
)

fun AppError.toDiagnosticEvent(severity: DiagnosticSeverity = DiagnosticSeverity.ERROR): DiagnosticEvent =
    DiagnosticEvent(
        severity = severity,
        code = code,
        operation = operation,
        diagnosticMessage = diagnosticMessage,
        cause = cause,
        context = context,
    )

fun interface DiagnosticLogger {
    fun record(event: DiagnosticEvent)
}

interface DiagnosticsStore {
    fun readText(): AppResult<String>

    fun clear(): AppResult<Unit>
}
