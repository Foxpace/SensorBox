package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecordingEngine(
    sources: List<RecordingSource>,
    private val scope: CoroutineScope,
    private val clock: RecordingClock,
    private val delay: RecordingDelay,
) {
    private val sourceByType = sources.associateBy(RecordingSource::type)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<RecordingSessionState>(RecordingSessionState.Idle)
    private val mutableEvents = MutableSharedFlow<RecordingEvent>(extraBufferCapacity = EVENT_BUFFER_SIZE)
    private var activeSources: List<RecordingSource> = emptyList()
    private var committingSessionId: RecordingSessionId? = null
    private var durationJob: Job? = null
    private var lastCompletedSessionId: RecordingSessionId? = null
    private var lastStopResult: AppResult<Unit> = AppResult.success(Unit)

    val state: StateFlow<RecordingSessionState> = mutableState.asStateFlow()
    val events: SharedFlow<RecordingEvent> = mutableEvents.asSharedFlow()

    suspend fun prepare(plan: RecordingPlan): AppResult<Unit> = mutex.withLock {
        if (mutableState.value !is RecordingSessionState.Idle) {
            return@withLock conflict("Prepare recording", plan.sessionId)
        }
        val validation = validate(plan)
        if (validation is AppResult.Failure) return@withLock reject(plan.sessionId, validation.error)

        mutableState.value = RecordingSessionState.Preparing(plan)
        val prepared = mutableListOf<RecordingSource>()
        for (spec in plan.sources.sortedBy { it.type.ordinal }) {
            val source = checkNotNull(sourceByType[spec.type])
            when (val result = source.prepare(spec)) {
                is AppResult.Success -> prepared += source

                is AppResult.Failure -> {
                    val sourcesToClean = prepared + source
                    activeSources = sourcesToClean
                    return@withLock rejectWithCleanup(plan.sessionId, result.error, sourcesToClean)
                }
            }
        }
        activeSources = prepared
        mutableState.value = RecordingSessionState.Prepared(plan)
        AppResult.success(Unit)
    }

    suspend fun commit(sessionId: RecordingSessionId): AppResult<Unit> = when (val claim = claimCommit(sessionId)) {
        is AppResult.Failure -> claim

        is AppResult.Success -> claim.value?.let { plan ->
            delay.pause((plan.startAtEpochMillis - clock.epochMillis()).coerceAtLeast(0L))
            startPreparedSources(sessionId, plan)
        } ?: AppResult.success(Unit)
    }

    private suspend fun claimCommit(sessionId: RecordingSessionId): AppResult<RecordingPlan?> = mutex.withLock {
        when (val current = mutableState.value) {
            is RecordingSessionState.Running -> if (current.plan.sessionId == sessionId) {
                AppResult.success(null)
            } else {
                conflict("Commit recording", sessionId)
            }

            is RecordingSessionState.Prepared -> claimPreparedCommit(current, sessionId)

            else -> conflict("Commit recording", sessionId)
        }
    }

    private fun claimPreparedCommit(
        current: RecordingSessionState.Prepared,
        sessionId: RecordingSessionId,
    ): AppResult<RecordingPlan?> = when {
        current.plan.sessionId != sessionId -> conflict("Commit recording", sessionId)

        committingSessionId == sessionId -> AppResult.success(null)

        else -> {
            committingSessionId = sessionId
            AppResult.success(current.plan)
        }
    }

    private suspend fun startPreparedSources(sessionId: RecordingSessionId, plan: RecordingPlan): AppResult<Unit> =
        mutex.withLock {
            val prepared = mutableState.value as? RecordingSessionState.Prepared
            if (prepared?.plan?.sessionId != sessionId) {
                return@withLock conflict("Commit recording", sessionId)
            }

            for (source in activeSources) {
                when (val result = source.start()) {
                    is AppResult.Success -> Unit

                    is AppResult.Failure -> {
                        committingSessionId = null
                        return@withLock rejectWithCleanup(sessionId, result.error, activeSources)
                    }
                }
            }
            committingSessionId = null
            val startedAt = clock.epochMillis()
            mutableState.value = RecordingSessionState.Running(plan, startedAt)
            mutableEvents.tryEmit(RecordingEvent.RecordingStarted(sessionId, startedAt))
            scheduleDurationStop(plan)
            AppResult.success(Unit)
        }

    suspend fun abort(sessionId: RecordingSessionId): AppResult<Unit> =
        stop(sessionId, RecordingStopReason.PAIRED_ABORT)

    suspend fun stop(sessionId: RecordingSessionId, reason: RecordingStopReason): AppResult<Unit> = mutex.withLock {
        val current = mutableState.value
        if (current is RecordingSessionState.Idle) {
            return@withLock if (lastCompletedSessionId == sessionId) {
                lastStopResult
            } else {
                conflict("Stop recording", sessionId)
            }
        }
        if (current.sessionId() != sessionId) return@withLock conflict("Stop recording", sessionId)

        mutableState.value = RecordingSessionState.Stopping(sessionId, reason)
        val currentJob = currentCoroutineContext()[Job]
        durationJob?.takeIf { it !== currentJob }?.cancel()
        durationJob = null
        committingSessionId = null
        val results = activeSources.asReversed().map { it.stop() }
        activeSources = emptyList()
        val result = results.combineAppResults(AppErrorCode.MEASUREMENT, "Stop recording sources")
        lastCompletedSessionId = sessionId
        lastStopResult = result
        mutableState.value = RecordingSessionState.Idle
        mutableEvents.tryEmit(RecordingEvent.RecordingStopped(sessionId, reason, result))
        result
    }

    private suspend fun rejectWithCleanup(
        sessionId: RecordingSessionId,
        startError: AppError,
        sources: List<RecordingSource>,
    ): AppResult<Unit> {
        val cleanup = sources.asReversed().map { it.stop() }
        activeSources = emptyList()
        val result = (listOf(AppResult.failure(startError)) + cleanup).combineAppResults(
            AppErrorCode.MEASUREMENT,
            "Reject recording start",
        )
        return reject(sessionId, checkNotNull(result.errorOrNull()))
    }

    private fun reject(sessionId: RecordingSessionId, error: AppError): AppResult<Unit> {
        committingSessionId = null
        mutableState.value = RecordingSessionState.Idle
        mutableEvents.tryEmit(RecordingEvent.RecordingStartRejected(sessionId, error))
        return AppResult.failure(error)
    }

    private fun validate(plan: RecordingPlan): AppResult<Unit> {
        val message = when {
            plan.sources.isEmpty() -> "Recording plan has no sources"

            plan.durationMillis < 0L -> "Recording duration is negative"

            plan.sources.map(RecordingSourceSpec::type).distinct().size != plan.sources.size ->
                "Recording plan repeats a source type"

            plan.sources.map(RecordingSourceSpec::type).any { it !in sourceByType } ->
                "Recording source adapter is missing"

            else -> null
        }
        return message?.let(::validationFailure) ?: AppResult.success(Unit)
    }

    private fun validationFailure(message: String): AppResult<Unit> = AppResult.failure(
        AppError(AppErrorCode.VALIDATION, "Validate recording plan", message),
    )

    private fun <T> conflict(operation: String, sessionId: RecordingSessionId): AppResult<T> = AppResult.failure(
        AppError(
            code = AppErrorCode.CONFLICT,
            operation = operation,
            diagnosticMessage = "$operation conflicts with the active session",
            context = mapOf("sessionId" to sessionId.value),
        ),
    )

    private fun scheduleDurationStop(plan: RecordingPlan) {
        if (plan.durationMillis <= 0L) return
        durationJob = scope.launch {
            delay.pause(plan.durationMillis)
            stop(plan.sessionId, RecordingStopReason.DURATION_EXPIRED)
        }
    }

    private fun RecordingSessionState.sessionId(): RecordingSessionId? = when (this) {
        RecordingSessionState.Idle -> null
        is RecordingSessionState.Preparing -> plan.sessionId
        is RecordingSessionState.Prepared -> plan.sessionId
        is RecordingSessionState.Running -> plan.sessionId
        is RecordingSessionState.Stopping -> sessionId
    }

    private companion object {
        const val EVENT_BUFFER_SIZE = 16
    }
}
