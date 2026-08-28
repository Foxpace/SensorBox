package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class RecordingEngine(
    sources: List<RecordingSource>,
    private val scope: CoroutineScope,
    private val waitFor: suspend (Long) -> Unit = { delay(it) },
) {
    private val sourceByType = sources.associateBy(RecordingSource::type)
    private val mutableEvents = MutableSharedFlow<RecordingEvent>(extraBufferCapacity = EVENT_BUFFER_SIZE)

    private var activeRecording: ActiveRecording? = null
    private var lastStopResult: AppResult<Unit>? = null

    val events: SharedFlow<RecordingEvent> = mutableEvents.asSharedFlow()

    @Suppress("ReturnCount")
    suspend fun start(plan: RecordingPlan): AppResult<Unit> {
        if (activeRecording != null) return conflict("Start recording", plan.sessionId)

        val validation = validate(plan)
        if (validation is AppResult.Failure) return validation

        val recording = ActiveRecording(plan)
        lastStopResult = null
        activeRecording = recording

        for (spec in plan.sources.sortedBy { it.type.ordinal }) {
            val source = checkNotNull(sourceByType[spec.type])
            recording.startedSources += source

            val result = source.start(spec)
            if (result is AppResult.Failure) return stopFailedStart(recording, result.error)
            if (activeRecording !== recording) return AppResult.success(Unit)
        }

        mutableEvents.tryEmit(RecordingEvent.RecordingStarted(plan.sessionId))
        scheduleDurationStop(recording)
        observeSourceFailures(recording)
        return AppResult.success(Unit)
    }

    suspend fun stop(reason: RecordingStopReason): AppResult<Unit> {
        val recording = activeRecording ?: return lastStopResult ?: conflict("Stop recording")
        return stopRecording(recording, RecordingStopContext(reason))
    }

    private suspend fun stopRecording(recording: ActiveRecording, context: RecordingStopContext): AppResult<Unit> {
        activeRecording = null
        recording.durationStop?.cancel()
        recording.failureMonitor?.cancel()

        val stoppedSources = stopSources(recording.startedSources, context)
        val results = buildList {
            context.failures.forEach { add(AppResult.failure(it)) }
            addAll(stoppedSources)
        }
        val result = results.combineAppResults(AppErrorCode.MEASUREMENT, "Stop recording sources")
        lastStopResult = result
        mutableEvents.tryEmit(
            RecordingEvent.RecordingStopped(
                sessionId = recording.plan.sessionId,
                reason = context.reason,
                result = result,
            ),
        )
        return result
    }

    private suspend fun stopFailedStart(recording: ActiveRecording, startError: AppError): AppResult<Unit> {
        if (activeRecording !== recording) return lastStopResult ?: AppResult.failure(startError)
        activeRecording = null
        val stoppedSources = stopSources(
            recording.startedSources,
            RecordingStopContext(RecordingStopReason.SOURCE_FAILURE, listOf(startError)),
        )
        val result = (listOf(AppResult.failure(startError)) + stoppedSources)
            .combineAppResults(AppErrorCode.MEASUREMENT, "Stop failed recording start")
        return result
    }

    private suspend fun stopSources(
        sources: List<RecordingSource>,
        context: RecordingStopContext,
    ): List<AppResult<Unit>> {
        var currentContext = context
        val results = mutableListOf<AppResult<Unit>>()
        for (source in sources.asReversed()) {
            val result = source.stop(currentContext)
            results += result
            result.errorOrNull()?.let { failure -> currentContext = currentContext.withFailure(failure) }
        }
        return results
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeSourceFailures(recording: ActiveRecording) {
        val failures = recording.startedSources.map(RecordingSource::failures)
        recording.failureMonitor = scope.launch {
            val failure = merge(*failures.toTypedArray()).first()
            if (activeRecording === recording) {
                recording.failureMonitor = null
                stopRecording(
                    recording = recording,
                    context = RecordingStopContext(RecordingStopReason.SOURCE_FAILURE, listOf(failure)),
                )
            }
        }
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

    private fun scheduleDurationStop(recording: ActiveRecording) {
        val plan = recording.plan
        if (plan.durationMillis <= 0L) return
        recording.durationStop = scope.launch {
            waitFor(plan.durationMillis)
            stop(RecordingStopReason.DURATION_EXPIRED)
        }
    }

    private fun <T> conflict(operation: String, sessionId: RecordingSessionId? = null): AppResult<T> =
        AppResult.failure(
            AppError(
                code = AppErrorCode.CONFLICT,
                operation = operation,
                diagnosticMessage = "$operation conflicts with the current recording state",
                context = sessionId?.let { mapOf("sessionId" to it.value) }.orEmpty(),
            ),
        )

    private class ActiveRecording(
        val plan: RecordingPlan,
        val startedSources: MutableList<RecordingSource> = mutableListOf(),
        var durationStop: Job? = null,
        var failureMonitor: Job? = null,
    )

    private companion object {
        const val EVENT_BUFFER_SIZE = 16
    }
}
