package com.tomasrepcik.sensorbox.recordinghost.session

import android.content.Context
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingEngine
import com.tomasrepcik.sensorbox.recording.RecordingEvent
import com.tomasrepcik.sensorbox.recording.RecordingSessionId
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingStopReason
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingRequest
import com.tomasrepcik.sensorbox.recordinghost.sources.AndroidRecordingSources
import com.tomasrepcik.sensorbox.recordinghost.storage.MeasurementStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import com.tomasrepcik.sensorbox.recording.RecordingRequest as EngineRecordingRequest

internal class ServiceController(
    context: Context,
    private val request: RecordingRequest,
    scope: CoroutineScope,
    private val storage: MeasurementStorage,
    diagnosticLogger: DiagnosticLogger,
    clock: EpochClock,
) : RecordingSessionExecution {
    private val androidSources = AndroidRecordingSources(context, request, storage, diagnosticLogger, clock)
    private val sessionId = RecordingSessionId(request.sessionId)
    private val engine = RecordingEngine(
        sources = androidSources.sources,
        scope = scope,
    )

    override val events: SharedFlow<RecordingEvent> = engine.events

    override suspend fun start(): AppResult<Unit> {
        val directory = storage.createMeasurementDirectory(
            request.folderName,
            request.useInternalStorage,
        )
        if (directory is AppResult.Failure) return directory

        return engine.start(request.toEngineRequest(sessionId))
    }

    override suspend fun stop(reason: RecordingStopReason): AppResult<Unit> = engine.stop(reason)

    override fun annotate(timestampMillis: Long, text: String): AppResult<Unit> =
        androidSources.annotate(timestampMillis, text)

    override fun playAlarm(): AppResult<Unit> = androidSources.playAlarm()

    private fun RecordingRequest.toEngineRequest(sessionId: RecordingSessionId): EngineRecordingRequest {
        val specs = buildList {
            add(RecordingSourceSpec.SessionMetadata)
            if (sensorIds.isNotEmpty()) add(RecordingSourceSpec.Sensors(sensorIds, sensorSamplingPeriod))
            if (includesGps) add(RecordingSourceSpec.Gps(gpsIntervalSeconds, gpsMinDistanceMeters))
            if (activityRecognition) add(RecordingSourceSpec.ActivityRecognition(activityRecognitionPeriodSeconds))
            if (significantMotion) add(RecordingSourceSpec.SignificantMotion)
        }
        return EngineRecordingRequest(
            sessionId = sessionId,
            sources = specs,
            durationMillis = durationMillis,
        )
    }
}
