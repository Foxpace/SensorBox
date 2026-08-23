package com.motionapps.sensorservices.serviceController

import android.content.Context
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.recording.RecordingClock
import com.motionapps.sensorbox.recording.RecordingDelay
import com.motionapps.sensorbox.recording.RecordingEngine
import com.motionapps.sensorbox.recording.RecordingEvent
import com.motionapps.sensorbox.recording.RecordingPlan
import com.motionapps.sensorbox.recording.RecordingSessionId
import com.motionapps.sensorbox.recording.RecordingSourceSpec
import com.motionapps.sensorbox.recording.RecordingStopReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow

class ServiceController(context: Context, private val config: MeasurementConfig, scope: CoroutineScope) {
    private val androidSources = AndroidRecordingSources(context, config)
    private val sessionId = RecordingSessionId(config.sessionId)
    private val engine = RecordingEngine(
        sources = androidSources.sources,
        scope = scope,
        clock = RecordingClock(System::currentTimeMillis),
        delay = RecordingDelay { durationMillis -> delay(durationMillis) },
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
