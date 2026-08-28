package com.tomasrepcik.sensorbox.sensorservices.serviceController

import android.content.Context
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingEngine
import com.tomasrepcik.sensorbox.recording.RecordingEvent
import com.tomasrepcik.sensorbox.recording.RecordingPlan
import com.tomasrepcik.sensorbox.recording.RecordingSessionId
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingStopReason
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementLaunchRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow

internal class ServiceController(
    context: Context,
    private val request: MeasurementLaunchRequest,
    scope: CoroutineScope,
    storage: MeasurementStorage,
    diagnosticLogger: DiagnosticLogger,
    clock: EpochClock,
) {
    private val androidSources = AndroidRecordingSources(context, request, storage, diagnosticLogger, clock)
    private val sessionId = RecordingSessionId(request.sessionId)
    private val engine = RecordingEngine(
        sources = androidSources.sources,
        scope = scope,
    )

    val events: SharedFlow<RecordingEvent> = engine.events

    suspend fun start(): AppResult<Unit> = engine.start(request.toRecordingPlan(sessionId))

    suspend fun stop(reason: RecordingStopReason): AppResult<Unit> = engine.stop(reason)

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = androidSources.annotate(timestampMillis, text)

    fun playAlarm(): AppResult<Unit> = androidSources.playAlarm()

    private fun MeasurementLaunchRequest.toRecordingPlan(sessionId: RecordingSessionId): RecordingPlan {
        val specs = buildList {
            add(RecordingSourceSpec.Session)
            if (sensorIds.isNotEmpty()) add(RecordingSourceSpec.Sensors(sensorIds, sensorSamplingPeriod))
            if (includesGps) add(RecordingSourceSpec.Gps(gpsIntervalSeconds, gpsMinDistanceMeters))
            if (activityRecognition) add(RecordingSourceSpec.ActivityRecognition(activityRecognitionPeriodSeconds))
            if (significantMotion) add(RecordingSourceSpec.SignificantMotion)
        }
        return RecordingPlan(
            sessionId = sessionId,
            sources = specs,
            durationMillis = durationMillis,
        )
    }
}
