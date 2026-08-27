package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class RecordingEngine(
    sources: List<RecordingSource>,
    private val scope: CoroutineScope,
    private val clock: RecordingClock,
    private val delay: RecordingDelay,
) {
    private val sourceByType = sources.associateBy(RecordingSource::type)
    private val mutableEvents = MutableSharedFlow<RecordingEvent>(extraBufferCapacity = EVENT_BUFFER_SIZE)

    private var activeRecording: ActiveRecording? = null
    private var durationStop: Job? = null
    private var lastStoppedSessionId: RecordingSessionId? = null
    private var lastStopResult: AppResult<Unit> = AppResult.success(Unit)

    val events: SharedFlow<RecordingEvent> = mutableEvents.asSharedFlow()

    @Suppress("ReturnCount")
    suspend fun start(plan: RecordingPlan): AppResult<Unit> {
        if (activeRecording != null) return conflict("Start recording", plan.sessionId)

        val validation = validate(plan)
        if (validation is AppResult.Failure) return rejectStart(plan.sessionId, validation.error)

        val recording = ActiveRecording(plan)
        activeRecording = recording

        for (spec in plan.sources.sortedBy { it.type.ordinal }) {
            val source = checkNotNull(sourceByType[spec.type])
            recording.startedSources += source

            val result = source.start(spec)
            if (result is AppResult.Failure) return stopFailedStart(recording, result.error)
            if (activeRecording !== recording) return AppResult.success(Unit)
        }

        val startedAt = clock.epochMillis()
        mutableEvents.tryEmit(RecordingEvent.RecordingStarted(plan.sessionId, startedAt))
        scheduleDurationStop(plan)
        return AppResult.success(Unit)
    }

    suspend fun stop(sessionId: RecordingSessionId, reason: RecordingStopReason): AppResult<Unit> {
        val recording = activeRecording
        if (recording == null) {
            return if (lastStoppedSessionId == sessionId) lastStopResult else conflict("Stop recording", sessionId)
        }
        if (recording.plan.sessionId != sessionId) return conflict("Stop recording", sessionId)

        activeRecording = null
        durationStop?.cancel()
        durationStop = null

        val stoppedSources = stopSources(recording.startedSources)
        val result = stoppedSources.combineAppResults(AppErrorCode.MEASUREMENT, "Stop recording sources")
        lastStoppedSessionId = sessionId
        lastStopResult = result
        mutableEvents.tryEmit(RecordingEvent.RecordingStopped(sessionId, reason, result))
        return result
    }

    private suspend fun stopFailedStart(recording: ActiveRecording, startError: AppError): AppResult<Unit> {
        if (activeRecording === recording) activeRecording = null
        val stoppedSources = stopSources(recording.startedSources)
        val result = (listOf(AppResult.failure(startError)) + stoppedSources)
            .combineAppResults(AppErrorCode.MEASUREMENT, "Stop failed recording start")
        return rejectStart(recording.plan.sessionId, checkNotNull(result.errorOrNull()))
    }

    private suspend fun stopSources(sources: List<RecordingSource>): List<AppResult<Unit>> =
        sources.asReversed().map { source -> source.stop() }

    private fun rejectStart(sessionId: RecordingSessionId, error: AppError): AppResult<Unit> {
        mutableEvents.tryEmit(RecordingEvent.RecordingStartRejected(sessionId, error))
        return AppResult.failure(error)
    }

    private fun validate(plan: RecordingPlan): AppResult<Unit> {
        val invalidReason = when {
            plan.sources.isEmpty() -> "Recording plan has no sources"

            plan.durationMillis < 0L -> "Recording duration is negative"

            plan.sources.map(RecordingSourceSpec::type).distinct().size != plan.sources.size ->
                "Recording plan repeats a source type"

            plan.sources.any { it.type !in sourceByType } -> "Recording source adapter is missing"

            else -> null
        }

        return if (invalidReason == null) {
            AppResult.success(Unit)
        } else {
            AppResult.failure(AppError(AppErrorCode.VALIDATION, "Validate recording plan", invalidReason))
        }
    }

    private fun scheduleDurationStop(plan: RecordingPlan) {
        if (plan.durationMillis <= 0L) return
        durationStop = scope.launch {
            delay.pause(plan.durationMillis)
            stop(plan.sessionId, RecordingStopReason.DURATION_EXPIRED)
        }
    }

    private fun <T> conflict(operation: String, sessionId: RecordingSessionId): AppResult<T> = AppResult.failure(
        AppError(
            code = AppErrorCode.CONFLICT,
            operation = operation,
            diagnosticMessage = "$operation conflicts with the active recording",
            context = mapOf("sessionId" to sessionId.value),
        ),
    )

    private data class ActiveRecording(
        val plan: RecordingPlan,
        val startedSources: MutableList<RecordingSource> = mutableListOf(),
    )

    private companion object {
        const val EVENT_BUFFER_SIZE = 16
    }
}
