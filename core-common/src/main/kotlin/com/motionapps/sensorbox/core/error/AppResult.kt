package com.motionapps.sensorbox.core.error

import kotlinx.coroutines.CancellationException

sealed interface AppResult<out T> {
    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun errorOrNull(): AppError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    fun <R> fold(onSuccess: (T) -> R, onFailure: (AppError) -> R): R = when (this) {
        is Success -> onSuccess(value)
        is Failure -> onFailure(error)
    }

    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> success(transform(value))
        is Failure -> this
    }

    fun getOrElse(onFailure: (AppError) -> @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> onFailure(error)
    }

    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> defaultValue
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw IllegalStateException(error.diagnosticMessage, error.cause)
    }

    fun onSuccess(action: (T) -> Unit): AppResult<T> = apply {
        if (this is Success) action(value)
    }

    fun onFailure(action: (AppError) -> Unit): AppResult<T> = apply {
        if (this is Failure) action(error)
    }

    data class Success<T>(val value: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>

    companion object {
        fun <T> success(value: T): AppResult<T> = Success(value)

        fun failure(error: AppError): AppResult<Nothing> = Failure(error)
    }
}

inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Success -> transform(value)
    is AppResult.Failure -> this
}

suspend inline fun <T, R> AppResult<T>.suspendFlatMap(
    crossinline transform: suspend (T) -> AppResult<R>,
): AppResult<R> = when (this) {
    is AppResult.Success -> transform(value)
    is AppResult.Failure -> this
}

@Suppress("TooGenericExceptionCaught")
inline fun <T> appResult(code: AppErrorCode, operation: String, block: () -> T): AppResult<T> = try {
    AppResult.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    AppResult.failure(AppError.from(code, operation, error))
}

@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> suspendAppResult(
    code: AppErrorCode,
    operation: String,
    crossinline block: suspend () -> T,
): AppResult<T> = try {
    AppResult.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    AppResult.failure(AppError.from(code, operation, error))
}

fun <T> AppResult<T>.withAppError(code: AppErrorCode, operation: String): AppResult<T> = when (this) {
    is AppResult.Success -> this
    is AppResult.Failure -> AppResult.failure(error.within(operation, code))
}

fun Iterable<AppResult<*>>.combineAppResults(code: AppErrorCode, operation: String): AppResult<Unit> {
    val errors = mapNotNull(AppResult<*>::errorOrNull)
    if (errors.isEmpty()) return AppResult.success(Unit)
    val causes = errors.mapNotNull(AppError::cause)
    val firstCause = causes.firstOrNull()
    causes.drop(1).forEach { cause -> firstCause?.addSuppressed(cause) }
    return AppResult.failure(
        AppError(
            code = errors.first().code.takeUnless { it == AppErrorCode.UNKNOWN } ?: code,
            operation = operation,
            diagnosticMessage = "$operation failed in ${errors.size} operation(s)",
            cause = firstCause,
            context = mapOf(
                "failureCount" to errors.size.toString(),
                "failedOperations" to errors.joinToString(",") { it.operation },
            ),
        ),
    )
}
