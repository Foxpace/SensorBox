package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.combineAppResults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield

class RecordingEngine(
    sources: List<RecordingSource>,
    private val scope: CoroutineScope,
    private val waitFor: suspend (Long) -> Unit = { delay(it) },
    private val sourceStartDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val sourceByType = sources.associateBy(RecordingSource::type)
    private val mutableEvents = MutableSharedFlow<RecordingEvent>(extraBufferCapacity = EVENT_BUFFER_SIZE)

    private var activeRecording: ActiveRecording? = null
    private var lastStopResult: AppResult<Unit>? = null

    val events: SharedFlow<RecordingEvent> = mutableEvents.asSharedFlow()

    @Suppress("ReturnCount")
    fun start(request: RecordingRequest): AppResult<Unit> {
        if (activeRecording != null) return conflict("Start recording", request.sessionId)

        val validation = validate(request)
        if (validation is AppResult.Failure) return validation

        val recording = ActiveRecording(request)
        lastStopResult = null
        activeRecording = recording

        val sourceSpecs = request.sources.sortedBy { it.type.ordinal }
        recording.startedSources += sourceSpecs.map { spec -> checkNotNull(sourceByType[spec.type]) }
        observeSourceFailures(recording)
        scheduleDurationStop(recording)
        launchRecordingSources(recording, sourceSpecs)
        return AppResult.success(Unit)
    }

    suspend fun stop(reason: RecordingStopReason): AppResult<Unit> {
        val recording = activeRecording ?: return lastStopResult ?: conflict("Stop recording")
        return stopRecording(recording, RecordingStopContext(reason))
    }

    private suspend fun stopRecording(recording: ActiveRecording, context: RecordingStopContext): AppResult<Unit> {
        cancelRecordingJobs(recording)
        val stoppedSources = stopRecordingSources(recording.startedSources, context)
        val results = buildList {
            context.failures.forEach { add(AppResult.failure(it)) }
            addAll(stoppedSources)
        }
        val result = results.combineAppResults(AppErrorCode.RECORDING, "Stop recording sources")
        lastStopResult = result
        publishRecordingStopped(recording, context.reason, result)
        return result
    }

    private fun cancelRecordingJobs(recording: ActiveRecording) {
        activeRecording = null
        recording.durationStop?.cancel()
        recording.failureMonitor?.cancel()
        recording.sourceStartJobs.values.toList().forEach(Job::cancel)
        recording.sourceStartJobs.clear()
    }

    private fun publishRecordingStopped(
        recording: ActiveRecording,
        reason: RecordingStopReason,
        result: AppResult<Unit>,
    ) {
        mutableEvents.tryEmit(
            RecordingEvent.RecordingStopped(
                sessionId = recording.request.sessionId,
                reason = reason,
                result = result,
            ),
        )
    }

    private suspend fun stopRecordingSources(
        sources: List<RecordingSource>,
        context: RecordingStopContext,
    ): List<AppResult<Unit>> {
        var currentContext = context
        val results = mutableListOf<AppResult<Unit>>()
        for (source in sources.asReversed()) {
            val result = stopRecordingSource(source, currentContext)
            results += result
            result.errorOrNull()?.let { failure -> currentContext = currentContext.withFailure(failure) }
        }
        return results
    }

    private suspend fun stopRecordingSource(source: RecordingSource, context: RecordingStopContext): AppResult<Unit> =
        withTimeoutOrNull(SOURCE_STOP_TIMEOUT_MILLIS) {
            source.stop(context)
        } ?: sourceStopTimeout(source)

    private fun sourceStopTimeout(source: RecordingSource): AppResult<Unit> = AppResult.failure(
        AppError(
            code = AppErrorCode.RECORDING,
            operation = "Stop ${source.type} recording source",
            diagnosticMessage = "Stop timed out after $SOURCE_STOP_TIMEOUT_MILLIS ms",
            context = mapOf(
                "sourceType" to source.type.name,
                "timeoutMillis" to SOURCE_STOP_TIMEOUT_MILLIS.toString(),
            ),
            isRetryable = true,
        ),
    )

    private fun launchRecordingSources(recording: ActiveRecording, specs: List<RecordingSourceSpec>) {
        for (spec in specs) {
            val source = checkNotNull(sourceByType[spec.type])
            val job = scope.launch(start = CoroutineStart.LAZY) {
                yield()
                val result = startRecordingSource(source, spec)
                recording.sourceStartJobs.remove(source)
                if (result is AppResult.Failure && activeRecording === recording) {
                    handleRecordingSourceFailure(recording, FailedSource(source, result.error))
                }
            }
            recording.sourceStartJobs[source] = job
            job.invokeOnCompletion { recording.sourceStartJobs.remove(source) }
            job.start()
        }
    }

    private suspend fun startRecordingSource(source: RecordingSource, spec: RecordingSourceSpec): AppResult<Unit> =
        withContext(sourceStartDispatcher) {
            source.start(spec)
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeSourceFailures(recording: ActiveRecording) {
        val failures = recording.startedSources.map { source ->
            source.failures.map { failure -> FailedSource(source, failure) }
        }
        recording.failureMonitor = scope.launch {
            merge(*failures.toTypedArray()).collect { failedSource ->
                if (activeRecording === recording) {
                    handleRecordingSourceFailure(recording, failedSource)
                }
            }
        }
    }

    private suspend fun handleRecordingSourceFailure(recording: ActiveRecording, failedSource: FailedSource) {
        if (!recording.startedSources.remove(failedSource.source)) return
        recording.sourceStartJobs.remove(failedSource.source)?.cancel()

        val stopResult = stopRecordingSource(
            failedSource.source,
            RecordingStopContext(RecordingStopReason.SOURCE_FAILURE, listOf(failedSource.failure)),
        )
        mutableEvents.emit(
            RecordingEvent.SourceFailed(
                sessionId = recording.request.sessionId,
                sourceType = failedSource.source.type,
                failure = failedSource.failure,
                stopFailure = stopResult.errorOrNull(),
            ),
        )
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
            if (activeRecording === recording) {
                recording.durationStop = null
                stopRecording(
                    recording = recording,
                    context = RecordingStopContext(RecordingStopReason.DURATION_EXPIRED),
                )
            }
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
        val sourceStartJobs: MutableMap<RecordingSource, Job> = mutableMapOf(),
        var durationStop: Job? = null,
        var failureMonitor: Job? = null,
    )

    private data class FailedSource(val source: RecordingSource, val failure: AppError)

    private companion object {
        const val EVENT_BUFFER_SIZE = 16
        const val SOURCE_STOP_TIMEOUT_MILLIS = 5_000L
    }
}
