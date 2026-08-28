package com.tomasrepcik.sensorbox.sensorservices.serviceController

import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ExtraInfoHandler
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementLaunchRequest
import com.tomasrepcik.sensorbox.sensorservices.types.SensorFileStats

internal class SessionRecordingSource(
    private val request: MeasurementLaunchRequest,
    private val storage: MeasurementStorage,
    clock: EpochClock,
    sensorManager: SensorManager,
) : RecordingSource {
    private val extraInfo = ExtraInfoHandler(storage, clock, sensorManager)
    private var toneGenerator: ToneGenerator? = null

    override val type = RecordingSourceType.SESSION

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> = if (spec is RecordingSourceSpec.Session) {
        storage.createMeasurementDirectory(request.folderName, request.useInternalStorage)
            .onSuccess { extraInfo.start(request) }
    } else {
        invalidSpec(type)
    }

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = appResult(
        AppErrorCode.MEASUREMENT,
        "Add measurement annotation",
    ) {
        extraInfo.annotate(timestampMillis, text)
    }

    fun playAlarm(): AppResult<Unit> = appResult(AppErrorCode.MEASUREMENT, "Play measurement alarm") {
        extraInfo.alarmTriggered()
        val tone = toneGenerator ?: ToneGenerator(AudioManager.STREAM_ALARM, 100).also { toneGenerator = it }
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ALARM_DURATION_MILLIS)
    }

    fun recordSensorStats(stats: List<SensorFileStats>) {
        extraInfo.recordSensorStats(stats)
    }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> {
        val releaseTone = appResult(AppErrorCode.MEASUREMENT, "Release measurement alarm") {
            toneGenerator?.release()
            toneGenerator = null
        }
        val metadataContext = releaseTone.errorOrNull()
            ?.let(context::withFailure)
            ?: context
        return listOf(releaseTone, extraInfo.write(metadataContext))
            .combineAppResults(AppErrorCode.MEASUREMENT, "Close measurement session artifacts")
    }

    private companion object {
        const val ALARM_DURATION_MILLIS = 1_000
    }
}
