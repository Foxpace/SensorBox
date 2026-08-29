package com.tomasrepcik.sensorbox.sensorservices.services

import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.toDiagnosticEvent
import com.tomasrepcik.sensorbox.recording.RecordingEvent
import com.tomasrepcik.sensorbox.recording.RecordingStopReason
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStopReason
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal interface RecordingSessionExecution {
    val events: SharedFlow<RecordingEvent>

    suspend fun start(): AppResult<Unit>

    suspend fun stop(reason: RecordingStopReason): AppResult<Unit>

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit>

    fun playAlarm(): AppResult<Unit>
}

internal interface RecordingHostEnvironment {
    fun start(request: RecordingRequest)

    fun scheduleAlarms(offsetsSeconds: List<Int>)

    fun release(): AppResult<Unit>

    fun removeNotification()

    fun stopService()
}

internal class RecordingHostSession(
    private val request: RecordingRequest,
    private val execution: RecordingSessionExecution,
    private val environment: RecordingHostEnvironment,
    private val sessionStore: RecordingSessionStore,
    private val diagnosticLogger: DiagnosticLogger,
    private val scope: CoroutineScope,
    private val elapsedRealtimeMillis: () -> Long,
) {
    private var eventJob: Job? = null
    private var isFinishing = false

    suspend fun start(): AppResult<Unit> {
        val hostStart = appResult(AppErrorCode.RECORDING, "Start recording foreground host") {
            environment.start(request)
        }
        if (hostStart.isFailure) {
            finish(RecordingSessionStopReason.SOURCE_FAILURE, hostStart)
            return hostStart
        }

        observeExecution()
        val result = execution.start()
        if (result.isFailure) finish(RecordingSessionStopReason.SOURCE_FAILURE, result)
        return result
    }

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = execution.annotate(timestampMillis, text)

    fun requestStop(reason: RecordingStopReason) {
        if (isFinishing) return
        sessionStore.markStopping()
        scope.launch { execution.stop(reason) }
    }

    fun destroy() {
        if (isFinishing) {
            eventJob?.cancel()
            return
        }

        isFinishing = true
        val executionResult = runBlocking(Dispatchers.IO) {
            execution.stop(RecordingStopReason.PLATFORM_DESTROYED)
        }
        val environmentResult = environment.release()
        sessionStore.markIdle()
        sessionStore.publishStopped(
            request.sessionId,
            RecordingSessionStopReason.SERVICE_DESTROYED,
            listOf(executionResult, environmentResult).combineAppResults(
                AppErrorCode.RECORDING,
                "Destroy recording foreground host",
            ),
        )
        eventJob?.cancel()
    }

    private fun observeExecution() {
        eventJob?.cancel()
        eventJob = scope.launch {
            execution.events.collect { event ->
                when (event) {
                    is RecordingEvent.RecordingStarted -> onStarted(event)
                    is RecordingEvent.SourceFailed -> recordSourceFailure(event)
                    is RecordingEvent.RecordingStopped -> finish(event.reason.toSessionReason(), event.result)
                }
            }
        }
    }

    private fun recordSourceFailure(event: RecordingEvent.SourceFailed) {
        diagnosticLogger.record(event.failure.toDiagnosticEvent())
        event.stopFailure?.let { diagnosticLogger.record(it.toDiagnosticEvent()) }
    }

    private fun onStarted(event: RecordingEvent.RecordingStarted) {
        sessionStore.markRunning(
            RecordingSessionState.Running(
                sessionId = event.sessionId.value,
                folderName = request.folderName,
                startedAtElapsedRealtime = elapsedRealtimeMillis(),
                sensorIds = request.sensorIds.toList(),
                includesGps = request.includesGps,
            ),
        )
        environment.scheduleAlarms(request.alarmOffsetsSeconds)
    }

    private suspend fun finish(reason: RecordingSessionStopReason, executionResult: AppResult<Unit>) {
        if (isFinishing) return
        isFinishing = true

        val hostResult = finishHost()
        val result = listOf(executionResult, hostResult)
            .combineAppResults(AppErrorCode.RECORDING, "Finish recording foreground host")
        sessionStore.publishStopped(request.sessionId, reason, result)
        eventJob?.cancel()
        eventJob = null
    }

    private fun finishHost(): AppResult<Unit> {
        val results = mutableListOf<AppResult<*>>()
        results += environment.release()
        results += appResult(AppErrorCode.RECORDING, "Publish idle recording state") {
            sessionStore.markIdle()
        }
        results += appResult(AppErrorCode.RECORDING, "Remove recording notification") {
            environment.removeNotification()
        }
        results += appResult(AppErrorCode.RECORDING, "Stop recording service instance") {
            environment.stopService()
        }
        return results.combineAppResults(AppErrorCode.RECORDING, "Finish recording host resources")
    }

    private fun RecordingStopReason.toSessionReason(): RecordingSessionStopReason = when (this) {
        RecordingStopReason.USER_REQUEST -> RecordingSessionStopReason.USER_REQUEST
        RecordingStopReason.DURATION_EXPIRED -> RecordingSessionStopReason.DURATION_EXPIRED
        RecordingStopReason.LOW_BATTERY -> RecordingSessionStopReason.LOW_BATTERY
        RecordingStopReason.SOURCE_FAILURE -> RecordingSessionStopReason.SOURCE_FAILURE
        RecordingStopReason.PLATFORM_DESTROYED -> RecordingSessionStopReason.SERVICE_DESTROYED
    }
}
