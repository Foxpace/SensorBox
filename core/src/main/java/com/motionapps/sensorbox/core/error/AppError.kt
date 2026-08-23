package com.motionapps.sensorbox.core.error

import kotlinx.coroutines.CancellationException

class AppError(val kind: Kind, val operation: String, cause: Throwable? = null) :
    Exception(message(operation, cause), cause) {
    init {
        AppDiagnostics.record(this)
    }

    enum class Kind {
        CONNECTIVITY,
        EXTERNAL_ACTION,
        MEASUREMENT,
        PERMISSION,
        PREFERENCES,
        STORAGE,
        UNKNOWN,
    }

    companion object {
        fun from(kind: Kind, operation: String, cause: Throwable): AppError =
            cause as? AppError ?: AppError(kind, operation, cause)

        private fun message(operation: String, cause: Throwable?): String =
            cause?.message?.takeIf(String::isNotBlank)?.let { "$operation: $it" } ?: "$operation failed"
    }
}

@Suppress("TooGenericExceptionCaught")
inline fun <T> appResult(kind: AppError.Kind, operation: String, block: () -> T): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    Result.failure(AppError.from(kind, operation, error))
}

@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> suspendAppResult(
    kind: AppError.Kind,
    operation: String,
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    Result.failure(AppError.from(kind, operation, error))
}

fun <T> Result<T>.withAppError(kind: AppError.Kind, operation: String): Result<T> = fold(
    onSuccess = Result.Companion::success,
    onFailure = { Result.failure(AppError.from(kind, operation, it)) },
)

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = fold(
    onSuccess = transform,
    onFailure = Result.Companion::failure,
)

suspend inline fun <T, R> Result<T>.suspendFlatMap(crossinline transform: suspend (T) -> Result<R>): Result<R> = fold(
    onSuccess = { transform(it) },
    onFailure = Result.Companion::failure,
)

fun Iterable<Result<*>>.combineAppResults(kind: AppError.Kind, operation: String): Result<Unit> {
    val failures = mapNotNull(Result<*>::exceptionOrNull)
    if (failures.isEmpty()) return Result.success(Unit)
    val first = failures.first()
    failures.drop(1).forEach(first::addSuppressed)
    return Result.failure(AppError.from(kind, operation, first))
}
