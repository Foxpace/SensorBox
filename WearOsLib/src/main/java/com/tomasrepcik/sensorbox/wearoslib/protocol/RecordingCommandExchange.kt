package com.tomasrepcik.sensorbox.wearoslib.protocol

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

fun interface RecordingCommandSender {
    suspend fun send(command: WearCommand): AppResult<Unit>
}

fun interface RecordingResultReceiver {
    fun receive(result: WearCommand.RecordingResult)
}

class RecordingCommandExchange(
    private val sendCommand: SendWearCommandUseCase,
    private val peerCapability: String,
    private val peerPath: String,
    private val peerName: String,
) : RecordingCommandSender,
    RecordingResultReceiver {
    private val results = ConcurrentHashMap<ResultKey, WearCommand.RecordingResult>()
    private val updates = MutableSharedFlow<WearCommand.RecordingResult>(extraBufferCapacity = RESULT_BUFFER_SIZE)

    @Suppress("ReturnCount")
    override suspend fun send(command: WearCommand): AppResult<Unit> {
        val operation = command.recordingOperation()
            ?: return AppResult.failure(AppError(AppErrorCode.VALIDATION, "Exchange recording command"))
        val sessionId = checkNotNull(command.recordingSessionId())
        clear(sessionId, operation)

        var lastSendError: AppError? = null
        var commandWasSent = false
        repeat(ATTEMPT_COUNT) { retryCount ->
            when (val sent = sendCommand(peerCapability, peerPath, command)) {
                is AppResult.Failure -> lastSendError = sent.error

                is AppResult.Success -> {
                    commandWasSent = true
                    lastSendError = null
                    val result = withTimeoutOrNull(RESULT_TIMEOUT_MILLIS / ATTEMPT_COUNT) {
                        await(sessionId, operation)
                    }
                    if (result != null) return result.toAppResult(retryCount)
                }
            }
        }

        if (!commandWasSent && lastSendError != null) return AppResult.failure(checkNotNull(lastSendError))
        return timeout(sessionId, operation, commandWasSent)
    }

    override fun receive(result: WearCommand.RecordingResult) {
        results[result.key()] = result
        updates.tryEmit(result)
    }

    private fun clear(sessionId: String, operation: WearRecordingOperation) {
        results.remove(ResultKey(sessionId, operation))
    }

    private suspend fun await(sessionId: String, operation: WearRecordingOperation): WearCommand.RecordingResult {
        val key = ResultKey(sessionId, operation)
        results[key]?.let { return it }
        return updates.first { result -> result.key() == key }
    }

    private fun WearCommand.RecordingResult.toAppResult(retryCount: Int): AppResult<Unit> =
        if (outcome == WearRecordingOutcome.SUCCEEDED) {
            AppResult.success(Unit)
        } else {
            AppResult.failure(
                AppError(
                    code = errorCode ?: AppErrorCode.UNKNOWN,
                    operation = errorOperation ?: "Handle $peerName $operation result",
                    diagnosticMessage = errorMessage ?: "$peerName $operation failed",
                    context = errorContext + mapOf(
                        "source" to peerName,
                        "sessionId" to sessionId,
                        "retryCount" to retryCount.toString(),
                        "failureCount" to failureCount.toString(),
                        "protocolVersion" to WearCommandCodec.PROTOCOL_VERSION.toString(),
                    ),
                ),
            )
        }

    private fun timeout(
        sessionId: String,
        operation: WearRecordingOperation,
        commandWasSent: Boolean,
    ): AppResult<Unit> = AppResult.failure(
        AppError(
            code = AppErrorCode.TIMEOUT,
            operation = "Await $peerName $operation result",
            diagnosticMessage = "$peerName $operation result timed out",
            context = mapOf(
                "source" to peerName,
                "sessionId" to sessionId,
                "retryCount" to RETRY_COUNT.toString(),
                "protocolVersion" to WearCommandCodec.PROTOCOL_VERSION.toString(),
                "commandWasSent" to commandWasSent.toString(),
            ),
            isRetryable = true,
        ),
    )

    private fun WearCommand.RecordingResult.key() = ResultKey(sessionId, operation)

    private data class ResultKey(val sessionId: String, val operation: WearRecordingOperation)

    private companion object {
        const val RESULT_TIMEOUT_MILLIS = 5_000L
        const val RETRY_COUNT = 2
        const val ATTEMPT_COUNT = RETRY_COUNT + 1
        const val RESULT_BUFFER_SIZE = 16
    }
}

fun AppError.wasRecordingCommandSent(): Boolean =
    code == AppErrorCode.TIMEOUT && context["commandWasSent"] == true.toString()

private fun WearCommand.recordingOperation(): WearRecordingOperation? = when (this) {
    is WearCommand.StartRecording -> WearRecordingOperation.START
    is WearCommand.StopRecording -> WearRecordingOperation.STOP
    else -> null
}
