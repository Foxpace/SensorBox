package com.motionapps.sensorbox.domain.paired

import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.DiagnosticLogger
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.toDiagnosticEvent
import com.motionapps.sensorbox.core.time.EpochClock
import com.motionapps.sensorbox.domain.measurement.MeasurementRequest
import com.motionapps.sensorbox.domain.measurement.PhoneRecordingController
import com.motionapps.sensorbox.domain.measurement.PreparedPhoneRecording
import com.motionapps.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.motionapps.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.motionapps.wearoslib.protocol.SendWearCommandUseCase
import com.motionapps.wearoslib.protocol.WearAcknowledgementOutcome
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
import com.motionapps.wearoslib.protocol.WearRecordingRequest
import com.motionapps.wearoslib.protocol.WearSessionCommand
import com.motionapps.wearoslib.protocol.WearStopReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PairedRecordingSession(val sessionId: String, val controlsWear: Boolean)

class RecordingSessionIdFactory @Inject constructor() {
    fun create(): String = UUID.randomUUID().toString()
}

@Singleton
class PairedRecordingCoordinator @Inject constructor(
    private val localController: PhoneRecordingController,
    private val sendCommand: SendWearCommandUseCase,
    private val acknowledgementInbox: WearAcknowledgementInbox,
    private val sessionIdFactory: RecordingSessionIdFactory,
    private val diagnosticLogger: DiagnosticLogger,
    private val clock: EpochClock,
) {
    private val mutex = Mutex()
    private val mutableSession = MutableStateFlow<PairedRecordingSession?>(null)

    val session: StateFlow<PairedRecordingSession?> = mutableSession.asStateFlow()

    suspend fun start(request: MeasurementRequest): AppResult<Unit> = mutex.withLock {
        if (mutableSession.value != null) return@withLock conflict("Start recording")
        val sessionId = sessionIdFactory.create()
        when (val preparation = prepareBoth(sessionId, request)) {
            is AppResult.Failure -> record(preparation)

            is AppResult.Success -> when (val commit = commitBoth(preparation.value, request)) {
                is AppResult.Failure -> record(commit)

                is AppResult.Success -> {
                    mutableSession.value = PairedRecordingSession(sessionId, preparation.value.controlsWear)
                    AppResult.success(Unit)
                }
            }
        }
    }

    private suspend fun prepareBoth(sessionId: String, request: MeasurementRequest): AppResult<PairedPreparation> =
        when (val local = localController.prepare(sessionId, request)) {
            is AppResult.Failure -> local
            is AppResult.Success -> prepareWearIfNeeded(sessionId, request, local.value)
        }

    private suspend fun prepareWearIfNeeded(
        sessionId: String,
        request: MeasurementRequest,
        prepared: PreparedPhoneRecording,
    ): AppResult<PairedPreparation> {
        val controlsWear = request.wearSensorIds.isNotEmpty() || request.wearIncludesGps
        if (!controlsWear) return AppResult.success(PairedPreparation(prepared, controlsWear = false))
        val result = exchange(
            command = WearCommand.PrepareRecording(sessionId, prepared.toWearRequest(request)),
            expected = WearSessionCommand.PREPARE,
            timeoutMillis = PREPARE_TIMEOUT_MILLIS,
        )
        return when (result) {
            is AppResult.Success -> AppResult.success(PairedPreparation(prepared, controlsWear = true))

            is AppResult.Failure -> {
                compensate(sessionId, prepared, localWasCommitted = false)
                result
            }
        }
    }

    private suspend fun commitBoth(preparation: PairedPreparation, request: MeasurementRequest): AppResult<Unit> {
        val sessionId = preparation.prepared.sessionId
        val startAtEpochMillis = clock.nowMillis() + request.delaySeconds.coerceAtLeast(0) * 1_000L +
            if (preparation.controlsWear) PAIRED_START_LEAD_MILLIS else 0L
        val local = localController.commit(preparation.prepared, startAtEpochMillis)
        if (local is AppResult.Failure) {
            if (preparation.controlsWear) compensate(sessionId, preparation.prepared, localWasCommitted = false)
            return local
        }
        if (!preparation.controlsWear) return AppResult.success(Unit)
        val remote = exchange(
            command = WearCommand.CommitRecording(sessionId, startAtEpochMillis),
            expected = WearSessionCommand.COMMIT,
            timeoutMillis = COMMIT_TIMEOUT_MILLIS,
        )
        if (remote is AppResult.Failure) {
            compensate(sessionId, preparation.prepared, localWasCommitted = true)
        }
        return remote
    }

    suspend fun stop(reason: WearStopReason = WearStopReason.USER_REQUEST): AppResult<Unit> = mutex.withLock {
        val active = mutableSession.value
            ?: return@withLock localController.stopAny(reason).onFailure { record(it) }
        val localStop = localController.stop(active.sessionId, reason)
        val remoteStop = if (active.controlsWear) {
            exchange(
                command = WearCommand.StopRecording(active.sessionId, reason),
                expected = WearSessionCommand.STOP,
                timeoutMillis = COMMIT_TIMEOUT_MILLIS,
            )
        } else {
            AppResult.success(Unit)
        }
        mutableSession.value = null
        listOf(localStop, remoteStop)
            .combineAppResults(AppErrorCode.MEASUREMENT, "Stop paired recording")
            .onFailure { record(it) }
    }

    suspend fun onAutomaticLocalStop(sessionId: String, reason: WearStopReason): AppResult<Unit> = mutex.withLock {
        val active = mutableSession.value
        if (active?.sessionId != sessionId) return@withLock AppResult.success(Unit)
        val remoteStop = if (active.controlsWear) {
            exchange(
                command = WearCommand.StopRecording(active.sessionId, reason),
                expected = WearSessionCommand.STOP,
                timeoutMillis = COMMIT_TIMEOUT_MILLIS,
            )
        } else {
            AppResult.success(Unit)
        }
        mutableSession.value = null
        remoteStop.onFailure { record(it) }
    }

    suspend fun stopFromPeer(sessionId: String, reason: WearStopReason): AppResult<Unit> = mutex.withLock {
        val result = localController.stop(sessionId, reason)
        if (mutableSession.value?.sessionId == sessionId) mutableSession.value = null
        result.onFailure { record(it) }
    }

    private suspend fun exchange(
        command: WearCommand,
        expected: WearSessionCommand,
        timeoutMillis: Long,
    ): AppResult<Unit> {
        val sessionId = command.sessionId()
        acknowledgementInbox.clear(sessionId, expected)
        var lastSendError: AppError? = null
        repeat(ATTEMPT_COUNT) { retryCount ->
            when (val sent = sendCommand(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, command)) {
                is AppResult.Failure -> lastSendError = sent.error

                is AppResult.Success -> {
                    lastSendError = null
                    val acknowledgement = withTimeoutOrNull(timeoutMillis / ATTEMPT_COUNT) {
                        acknowledgementInbox.await(sessionId, expected)
                    }
                    if (acknowledgement != null) return acknowledgement.toResult(retryCount)
                }
            }
        }
        return lastSendError?.let { error -> AppResult.failure(error) } ?: AppResult.failure(
            AppError(
                code = AppErrorCode.TIMEOUT,
                operation = "Await Wear $expected acknowledgement",
                diagnosticMessage = "Wear $expected acknowledgement timed out",
                context = mapOf(
                    "sessionId" to sessionId,
                    "retryCount" to RETRY_COUNT.toString(),
                    "protocolVersion" to WearCommandCodec.PROTOCOL_VERSION.toString(),
                ),
                isRetryable = true,
            ),
        )
    }

    private suspend fun compensate(sessionId: String, prepared: PreparedPhoneRecording, localWasCommitted: Boolean) {
        if (localWasCommitted) localController.abort(prepared.sessionId) else localController.abort(sessionId)
        exchange(
            command = WearCommand.AbortRecording(sessionId),
            expected = WearSessionCommand.ABORT,
            timeoutMillis = COMMIT_TIMEOUT_MILLIS,
        )
    }

    private fun PreparedPhoneRecording.toWearRequest(request: MeasurementRequest) = WearRecordingRequest(
        folderName = launchRequest.folderName,
        sensorIds = request.wearSensorIds.sorted(),
        includesGps = request.wearIncludesGps,
        durationMillis = launchRequest.durationMillis,
        measurementType = launchRequest.measurementType,
    )

    private fun WearCommand.Acknowledgement.toResult(retryCount: Int): AppResult<Unit> =
        if (outcome == WearAcknowledgementOutcome.SUCCEEDED) {
            AppResult.success(Unit)
        } else {
            AppResult.failure(
                AppError(
                    code = errorCode ?: AppErrorCode.UNKNOWN,
                    operation = "Handle Wear $command acknowledgement",
                    diagnosticMessage = "Wear $command acknowledgement was $outcome",
                    context = mapOf(
                        "sessionId" to sessionId,
                        "retryCount" to retryCount.toString(),
                        "failureCount" to failureCount.toString(),
                        "protocolVersion" to WearCommandCodec.PROTOCOL_VERSION.toString(),
                    ),
                ),
            )
        }

    private fun WearCommand.sessionId(): String = when (this) {
        is WearCommand.PrepareRecording -> sessionId

        is WearCommand.CommitRecording -> sessionId

        is WearCommand.AbortRecording -> sessionId

        is WearCommand.StopRecording -> sessionId

        is WearCommand.Acknowledgement -> sessionId

        WearCommand.LaunchPhone,
        WearCommand.SyncMeasurements,
        WearCommand.RequestSensorList,
        is WearCommand.SensorList,
        -> error("Wear command has no recording session")
    }

    private fun conflict(operation: String): AppResult<Unit> = AppResult.failure(
        AppError(AppErrorCode.CONFLICT, operation),
    )

    private fun <T> record(result: AppResult<T>): AppResult<T> = result.onFailure { record(it) }

    private fun record(error: AppError) {
        diagnosticLogger.record(error.toDiagnosticEvent())
    }

    private data class PairedPreparation(val prepared: PreparedPhoneRecording, val controlsWear: Boolean)

    private companion object {
        const val PREPARE_TIMEOUT_MILLIS = 10_000L
        const val COMMIT_TIMEOUT_MILLIS = 5_000L
        const val PAIRED_START_LEAD_MILLIS = 6_000L
        const val RETRY_COUNT = 2
        const val ATTEMPT_COUNT = RETRY_COUNT + 1
    }
}
