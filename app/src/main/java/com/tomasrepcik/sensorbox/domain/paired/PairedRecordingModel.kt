package com.tomasrepcik.sensorbox.domain.paired

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.domain.recording.RecordingSetup
import com.tomasrepcik.sensorbox.domain.recording.StartedPhoneRecording
import com.tomasrepcik.sensorbox.domain.recording.toWatchRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOperation
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOutcome

internal sealed interface PairedRecordingState {
    data object Idle : PairedRecordingState

    data class Starting(val sessionId: String, val includesWatch: Boolean) : PairedRecordingState

    data class Recording(val sessionId: String, val includesWatch: Boolean) : PairedRecordingState

    data class Stopping(val sessionId: String, val includesWatch: Boolean) : PairedRecordingState
}

internal data class ActivePairedRecording(val sessionId: String, val includesWatch: Boolean)

internal fun PairedRecordingState.activeRecording(): ActivePairedRecording? = when (this) {
    is PairedRecordingState.Starting -> ActivePairedRecording(sessionId, includesWatch)
    is PairedRecordingState.Recording -> ActivePairedRecording(sessionId, includesWatch)
    is PairedRecordingState.Stopping -> ActivePairedRecording(sessionId, includesWatch)
    PairedRecordingState.Idle -> null
}

internal fun RecordingSetup.includesWatchRecording(): Boolean = watchSensorIds.isNotEmpty() || watchIncludesGps

internal fun StartedPhoneRecording.toWatchStartCommand(request: RecordingSetup) = WearCommand.StartRecording(
    sessionId = sessionId,
    request = toWatchRecordingRequest(request),
)

internal fun WearCommand.RecordingResult.toAppResult(retryCount: Int): AppResult<Unit> =
    if (outcome == WearRecordingOutcome.SUCCEEDED) {
        AppResult.success(Unit)
    } else {
        AppResult.failure(
            AppError(
                code = errorCode ?: AppErrorCode.UNKNOWN,
                operation = errorOperation ?: "Handle watch $operation result",
                diagnosticMessage = errorMessage ?: "Watch $operation failed",
                context = errorContext + mapOf(
                    "source" to "watch",
                    "sessionId" to sessionId,
                    "retryCount" to retryCount.toString(),
                    "failureCount" to failureCount.toString(),
                    "protocolVersion" to WearCommandCodec.PROTOCOL_VERSION.toString(),
                ),
            ),
        )
    }

internal fun WearRecordingOperation.timeoutOperation(): String = "Await watch $name result"
