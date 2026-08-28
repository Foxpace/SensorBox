package com.tomasrepcik.sensorbox.domain.paired

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.toDiagnosticEvent
import com.tomasrepcik.sensorbox.domain.recording.PhoneRecordingController
import com.tomasrepcik.sensorbox.domain.recording.RecordingSetup
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOperation
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearStopReason
import com.tomasrepcik.sensorbox.wearoslib.protocol.recordingSessionId
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairedRecordingCoordinator @Inject constructor(
    private val phoneRecording: PhoneRecordingController,
    private val sendCommand: SendWearCommandUseCase,
    private val watchResults: WatchRecordingResultInbox,
    private val diagnosticLogger: DiagnosticLogger,
) {
    private var state: PairedRecordingState = PairedRecordingState.Idle

    @Suppress("ReturnCount")
    suspend fun start(request: RecordingSetup): AppResult<Unit> {
        if (state !is PairedRecordingState.Idle) return conflict("Start recording")

        val sessionId = UUID.randomUUID().toString()
        val includesWatch = request.includesWatchRecording()
        val starting = PairedRecordingState.Starting(sessionId, includesWatch)
        state = starting

        val startedPhone = when (val phoneStart = phoneRecording.start(sessionId, request)) {
            is AppResult.Failure -> {
                state = PairedRecordingState.Idle
                return record(phoneStart)
            }

            is AppResult.Success -> phoneStart.value
        }

        val watchStart = if (includesWatch) {
            exchange(startedPhone.toWatchStartCommand(request), WearRecordingOperation.START)
        } else {
            AppResult.success(Unit)
        }

        if (state !== starting) return AppResult.success(Unit)
        if (watchStart is AppResult.Failure) {
            if (watchStart.error.wasWatchCommandSent()) {
                record(watchStart.error)
                state = PairedRecordingState.Recording(sessionId, includesWatch)
                return AppResult.success(Unit)
            }

            stopStartedRecording(sessionId, includesWatch, WearStopReason.SOURCE_FAILURE)
            return record(watchStart)
        }

        state = PairedRecordingState.Recording(sessionId, includesWatch)
        return AppResult.success(Unit)
    }

    suspend fun stop(reason: WearStopReason = WearStopReason.USER_REQUEST): AppResult<Unit> {
        val active = state.activeRecording()
            ?: return phoneRecording.stopCurrent(reason).onFailure(::record)
        state = PairedRecordingState.Stopping(active.sessionId, active.includesWatch)
        return stopStartedRecording(active.sessionId, active.includesWatch, reason)
    }

    suspend fun onAutomaticPhoneStop(sessionId: String, reason: WearStopReason): AppResult<Unit> {
        val active = state.activeRecording()
        if (active?.sessionId != sessionId) return AppResult.success(Unit)

        val watchStop = stopWatch(sessionId, active.includesWatch, reason)
        state = PairedRecordingState.Idle
        return watchStop.onFailure(::record)
    }

    suspend fun stopFromWatch(sessionId: String, reason: WearStopReason): AppResult<Unit> {
        if (state.activeRecording()?.sessionId != sessionId) return AppResult.success(Unit)

        val result = phoneRecording.stop(sessionId, reason)
        state = PairedRecordingState.Idle
        return result.onFailure(::record)
    }

    private suspend fun stopStartedRecording(
        sessionId: String,
        includesWatch: Boolean,
        reason: WearStopReason,
    ): AppResult<Unit> {
        val phoneStop = phoneRecording.stop(sessionId, reason)
        val watchStop = stopWatch(sessionId, includesWatch, reason)
        state = PairedRecordingState.Idle
        return listOf(phoneStop, watchStop)
            .combineAppResults(AppErrorCode.RECORDING, "Stop phone and watch recording")
            .onFailure(::record)
    }

    private suspend fun stopWatch(sessionId: String, includesWatch: Boolean, reason: WearStopReason): AppResult<Unit> =
        if (includesWatch) {
            exchange(WearCommand.StopRecording(sessionId, reason), WearRecordingOperation.STOP)
        } else {
            AppResult.success(Unit)
        }

    private suspend fun exchange(command: WearCommand, expectedOperation: WearRecordingOperation): AppResult<Unit> {
        val sessionId = checkNotNull(command.recordingSessionId())
        watchResults.clear(sessionId, expectedOperation)
        var lastSendError: AppError? = null
        var commandWasSent = false

        repeat(ATTEMPT_COUNT) { retryCount ->
            when (val sent = sendCommand(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, command)) {
                is AppResult.Failure -> lastSendError = sent.error

                is AppResult.Success -> {
                    commandWasSent = true
                    lastSendError = null
                    val result = withTimeoutOrNull(RESULT_TIMEOUT_MILLIS / ATTEMPT_COUNT) {
                        watchResults.await(sessionId, expectedOperation)
                    }
                    if (result != null) return result.toAppResult(retryCount)
                }
            }
        }

        if (!commandWasSent && lastSendError != null) return AppResult.failure(checkNotNull(lastSendError))

        return AppResult.failure(
            AppError(
                code = AppErrorCode.TIMEOUT,
                operation = expectedOperation.timeoutOperation(),
                diagnosticMessage = "watch $expectedOperation result timed out",
                context = mapOf(
                    "source" to "watch",
                    "sessionId" to sessionId,
                    "retryCount" to RETRY_COUNT.toString(),
                    "protocolVersion" to WearCommandCodec.PROTOCOL_VERSION.toString(),
                    "commandWasSent" to commandWasSent.toString(),
                ),
                isRetryable = true,
            ),
        )
    }

    private fun AppError.wasWatchCommandSent(): Boolean =
        code == AppErrorCode.TIMEOUT && context["commandWasSent"] == true.toString()

    private fun conflict(operation: String): AppResult<Unit> =
        AppResult.failure(AppError(AppErrorCode.CONFLICT, operation))

    private fun <T> record(result: AppResult<T>): AppResult<T> = result.onFailure(::record)

    private fun record(error: AppError) {
        diagnosticLogger.record(error.toDiagnosticEvent())
    }

    private companion object {
        const val RESULT_TIMEOUT_MILLIS = 5_000L
        const val RETRY_COUNT = 2
        const val ATTEMPT_COUNT = RETRY_COUNT + 1
    }
}
