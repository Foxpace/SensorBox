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
    suspend fun start(request: RecordingRequest): AppResult<Unit> {
        if (activeRecording != null) return conflict("Start recording", request.sessionId)

        val validation = validate(request)
        if (validation is AppResult.Failure) return validation

        val recording = ActiveRecording(request)
        lastStopResult = null
        activeRecording = recording

        for (spec in request.sources.sortedBy { it.type.ordinal }) {
            val source = checkNotNull(sourceByType[spec.type])
            recording.startedSources += source

            val result = source.start(spec)
            if (result is AppResult.Failure) return stopFailedStart(recording, result.error)
            if (activeRecording !== recording) return AppResult.success(Unit)
        }

        mutableEvents.tryEmit(RecordingEvent.RecordingStarted(request.sessionId))
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
        val result = results.combineAppResults(AppErrorCode.RECORDING, "Stop recording sources")
        lastStopResult = result
        mutableEvents.tryEmit(
            RecordingEvent.RecordingStopped(
                sessionId = recording.request.sessionId,
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
            .combineAppResults(AppErrorCode.RECORDING, "Stop failed recording start")
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

    private fun validate(request: RecordingRequest): AppResult<Unit> {
        val invalidReason = when {
            request.sources.isEmpty() -> "Recording request has no sources"

            request.durationMillis < 0L -> "Recording duration is negative"

            request.sources.map(RecordingSourceSpec::type).distinct().size != request.sources.size ->
                "Recording request repeats a source type"

            request.sources.any { it.type !in sourceByType } -> "Recording source adapter is missing"

            else -> null
        }

        return if (invalidReason == null) {
            AppResult.success(Unit)
        } else {
            AppResult.failure(AppError(AppErrorCode.VALIDATION, "Validate recording request", invalidReason))
        }
    }

    private fun scheduleDurationStop(recording: ActiveRecording) {
        val request = recording.request
        if (request.durationMillis <= 0L) return
        recording.durationStop = scope.launch {
            waitFor(request.durationMillis)
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
        val request: RecordingRequest,
        val startedSources: MutableList<RecordingSource> = mutableListOf(),
        var durationStop: Job? = null,
        var failureMonitor: Job? = null,
    )

    private companion object {
        const val EVENT_BUFFER_SIZE = 16
    }
}
