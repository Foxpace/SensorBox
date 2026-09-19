package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.failure.combineAppResults
import com.tomasrepcik.sensorbox.core.failure.toDiagnosticEvent
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.pairedrecording.PairedRecordingState
import com.tomasrepcik.sensorbox.pairedrecording.activeRecording
import com.tomasrepcik.sensorbox.pairedrecording.includesWatchRecording
import com.tomasrepcik.sensorbox.pairedrecording.toWatchStartCommand
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetup
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.RecordingCommandSender
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearStopReason
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.wasRecordingCommandSent
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface RecordingControlUseCase {
    suspend fun start(request: RecordingSetup): AppResult<Unit>

    suspend fun stop(): AppResult<Unit>

    fun annotate(text: String): AppResult<Unit>
}

interface PhoneRecordingSessionControl {
    suspend fun onAutomaticPhoneStop(sessionId: String, reason: WearStopReason): AppResult<Unit>
}

interface PeerRecordingControl {
    suspend fun stopFromWatch(sessionId: String, reason: WearStopReason): AppResult<Unit>
}

@Singleton
class DefaultRecordingControlUseCase @Inject constructor(
    private val phoneRecording: PhoneRecordingController,
    private val watchCommands: RecordingCommandSender,
    private val diagnosticLogger: DiagnosticLogger,
    private val clock: EpochClock,
) : RecordingControlUseCase,
    PhoneRecordingSessionControl,
    PeerRecordingControl {
    private var state: PairedRecordingState = PairedRecordingState.Idle

    @Suppress("ReturnCount")
    override suspend fun start(request: RecordingSetup): AppResult<Unit> {
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
            watchCommands.send(startedPhone.toWatchStartCommand(request))
        } else {
            AppResult.success(Unit)
        }

        if (state !== starting) return AppResult.success(Unit)
        if (watchStart is AppResult.Failure) {
            if (watchStart.error.wasRecordingCommandSent()) {
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

    override suspend fun stop(): AppResult<Unit> {
        val active = state.activeRecording()
            ?: return phoneRecording.stopCurrent(WearStopReason.USER_REQUEST).onFailure(::record)
        state = PairedRecordingState.Stopping(active.sessionId, active.includesWatch)
        return stopStartedRecording(active.sessionId, active.includesWatch, WearStopReason.USER_REQUEST)
    }

    override fun annotate(text: String): AppResult<Unit> = phoneRecording.annotate(text, clock.nowMillis())

    override suspend fun onAutomaticPhoneStop(sessionId: String, reason: WearStopReason): AppResult<Unit> {
        val active = state.activeRecording()
        if (active?.sessionId != sessionId) return AppResult.success(Unit)

        val watchStop = stopWatch(sessionId, active.includesWatch, reason)
        state = PairedRecordingState.Idle
        return watchStop.onFailure(::record)
    }

    override suspend fun stopFromWatch(sessionId: String, reason: WearStopReason): AppResult<Unit> {
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
            watchCommands.send(WearCommand.StopRecording(sessionId, reason))
        } else {
            AppResult.success(Unit)
        }

    private fun conflict(operation: String): AppResult<Unit> =
        AppResult.failure(AppError(AppErrorCode.CONFLICT, operation))

    private fun <T> record(result: AppResult<T>): AppResult<T> = result.onFailure(::record)

    private fun record(error: AppError) {
        diagnosticLogger.record(error.toDiagnosticEvent())
    }
}
