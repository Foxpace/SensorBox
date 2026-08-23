package com.motionapps.sensorbox.core.error

data class AppError(
    val code: AppErrorCode,
    val operation: String,
    val diagnosticMessage: String,
    val cause: Throwable? = null,
    val context: Map<String, String> = emptyMap(),
    val isRetryable: Boolean = false,
) {
    constructor(code: AppErrorCode, operation: String) : this(
        code = code,
        operation = operation,
        diagnosticMessage = "$operation failed",
    )

    fun within(parentOperation: String, fallbackCode: AppErrorCode = code): AppError = copy(
        code = if (code == AppErrorCode.UNKNOWN) fallbackCode else code,
        operation = parentOperation,
        diagnosticMessage = "$parentOperation failed during $operation",
        context = context + ("failedOperation" to operation),
    )

    companion object {
        fun from(code: AppErrorCode, operation: String, cause: Throwable): AppError = AppError(
            code = code,
            operation = operation,
            diagnosticMessage = "$operation failed with ${cause::class.java.simpleName}",
            cause = cause,
        )
    }
}

enum class AppErrorCode {
    CONNECTIVITY,
    EXTERNAL_ACTION,
    MEASUREMENT,
    PERMISSION,
    PREFERENCES,
    STORAGE,
    VALIDATION,
    TIMEOUT,
    CONFLICT,
    UNKNOWN,
}
