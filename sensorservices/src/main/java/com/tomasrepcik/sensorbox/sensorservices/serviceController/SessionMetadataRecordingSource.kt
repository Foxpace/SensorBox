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
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.MeasurementMetadataWriter
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest
import com.tomasrepcik.sensorbox.sensorservices.types.SensorFileStats

internal class SessionMetadataRecordingSource(
    private val request: RecordingRequest,
    private val storage: MeasurementStorage,
    clock: EpochClock,
    sensorManager: SensorManager,
) : RecordingSource {
    private val metadataWriter = MeasurementMetadataWriter(storage, clock, sensorManager)
    private var toneGenerator: ToneGenerator? = null

    override val type = RecordingSourceType.SESSION_METADATA

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.SessionMetadata) {
            storage.createMeasurementDirectory(request.folderName, request.useInternalStorage)
                .onSuccess { metadataWriter.start(request) }
        } else {
            invalidSpec(type)
        }

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = appResult(
        AppErrorCode.RECORDING,
        "Add measurement annotation",
    ) {
        metadataWriter.annotate(timestampMillis, text)
    }

    fun playAlarm(): AppResult<Unit> = appResult(AppErrorCode.RECORDING, "Play recording alarm") {
        metadataWriter.alarmTriggered()
        val tone = toneGenerator ?: ToneGenerator(AudioManager.STREAM_ALARM, 100).also { toneGenerator = it }
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ALARM_DURATION_MILLIS)
    }

    fun recordSensorStats(stats: List<SensorFileStats>) {
        metadataWriter.recordSensorStats(stats)
    }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> {
        val releaseTone = appResult(AppErrorCode.RECORDING, "Release recording alarm") {
            toneGenerator?.release()
            toneGenerator = null
        }
        val metadataContext = releaseTone.errorOrNull()
            ?.let(context::withFailure)
            ?: context
        return listOf(releaseTone, metadataWriter.write(metadataContext))
            .combineAppResults(AppErrorCode.RECORDING, "Close recording session metadata")
    }

    private companion object {
        const val ALARM_DURATION_MILLIS = 1_000
    }
}
