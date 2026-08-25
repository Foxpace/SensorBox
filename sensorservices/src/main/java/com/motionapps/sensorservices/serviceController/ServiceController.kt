package com.motionapps.sensorservices.serviceController

import android.content.Context
import com.motionapps.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingClock
import com.tomasrepcik.sensorbox.recording.RecordingDelay
import com.tomasrepcik.sensorbox.recording.RecordingEngine
import com.tomasrepcik.sensorbox.recording.RecordingEvent
import com.tomasrepcik.sensorbox.recording.RecordingPlan
import com.tomasrepcik.sensorbox.recording.RecordingSessionId
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingStopReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

internal class ServiceController(
    context: Context,
    private val config: MeasurementConfig,
    scope: CoroutineScope,
    storage: MeasurementStorage,
    diagnosticLogger: DiagnosticLogger,
    clock: EpochClock,
) {
    private val androidSources = AndroidRecordingSources(context, config, storage, diagnosticLogger, clock)
    private val sessionId = RecordingSessionId(config.sessionId)
    private val engine = RecordingEngine(
        sources = androidSources.sources,
        scope = scope,
        clock = RecordingClock(clock::nowMillis),
        delay = RecordingDelay { delayMillis -> delay(delayMillis) },
    )

    val events: SharedFlow<RecordingEvent> = engine.events

    suspend fun prepareAndCommit(): AppResult<Unit> {
        val plan = config.toRecordingPlan(sessionId)
        return when (val prepared = engine.prepare(plan)) {
            is AppResult.Success -> engine.commit(sessionId)
            is AppResult.Failure -> prepared
        }
    }

    suspend fun stop(reason: RecordingStopReason): AppResult<Unit> = engine.stop(sessionId, reason)

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = androidSources.annotate(timestampMillis, text)

    fun playAlarm(): AppResult<Unit> = androidSources.playAlarm()

    private fun MeasurementConfig.toRecordingPlan(sessionId: RecordingSessionId): RecordingPlan {
        val specs = buildList {
            add(RecordingSourceSpec.Session)
            if (sensorIds.isNotEmpty()) add(RecordingSourceSpec.Sensors(sensorIds.toSet(), sensorSamplingPeriod))
            if (includesGps) add(RecordingSourceSpec.Gps(gpsIntervalSeconds, gpsMinDistanceMeters))
            if (activityRecognition) add(RecordingSourceSpec.ActivityRecognition(activityRecognitionPeriodSeconds))
            if (significantMotion) add(RecordingSourceSpec.SignificantMotion)
        }
        return RecordingPlan(
            sessionId = sessionId,
            sources = specs,
            startAtEpochMillis = startAtEpochMillis,
            durationMillis = durationMillis,
        )
    }
}
