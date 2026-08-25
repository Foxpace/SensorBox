package com.tomasrepcik.sensorbox.sensorservices.serviceController

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.sensorservices.handlers.GPSHandler
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ActivityRecognitionMeasurement
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ActivityRecognitionPlatform
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.AndroidActivityRecognitionPlatform
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ExtraInfoHandler
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.GPSMeasurement
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SensorMeasurement
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SignificantMotion

internal class AndroidRecordingSources(
    private val context: Context,
    private val config: MeasurementConfig,
    storage: MeasurementStorage,
    diagnosticLogger: DiagnosticLogger,
    clock: EpochClock,
) {
    private val artifacts = SessionArtifacts(context, config, storage, clock)

    val sources: List<RecordingSource> = listOf(
        SessionSource(artifacts),
        SensorSource(context, config, storage, diagnosticLogger, clock),
        GpsSource(context, config, storage, clock),
        ActivitySource(config, storage, AndroidActivityRecognitionPlatform(context)),
        SignificantMotionSource(context, config, storage, clock),
    )

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = artifacts.annotate(timestampMillis, text)

    fun playAlarm(): AppResult<Unit> = artifacts.playAlarm()
}

private class SessionSource(private val artifacts: SessionArtifacts) : RecordingSource {
    override val type = RecordingSourceType.SESSION

    override suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.Session) artifacts.prepare() else invalidSpec(type)

    override suspend fun start(): AppResult<Unit> = AppResult.success(Unit)

    override suspend fun stop(): AppResult<Unit> = artifacts.stop()
}

private class SensorSource(
    private val context: Context,
    private val config: MeasurementConfig,
    storage: MeasurementStorage,
    diagnosticLogger: DiagnosticLogger,
    clock: EpochClock,
) : RecordingSource {
    private val measurement = SensorMeasurement(storage, diagnosticLogger, clock)
    override val type = RecordingSourceType.SENSOR

    override suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.Sensors) {
            measurement.prepare(
                folderName = config.folderName,
                useInternalStorage = config.useInternalStorage,
                sensorTypes = spec.sensorTypes,
                samplingPeriod = spec.samplingPeriod,
            )
        } else {
            invalidSpec(type)
        }

    override suspend fun start(): AppResult<Unit> = measurement.start(context)

    override suspend fun stop(): AppResult<Unit> = measurement.stop(context)
}

private class GpsSource(
    private val context: Context,
    private val config: MeasurementConfig,
    storage: MeasurementStorage,
    clock: EpochClock,
) : RecordingSource {
    private val measurement = GPSMeasurement(GPSHandler(), storage, clock)
    override val type = RecordingSourceType.GPS

    override suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit> = if (spec is RecordingSourceSpec.Gps) {
        measurement.prepare(
            folderName = config.folderName,
            useInternalStorage = config.useInternalStorage,
            intervalSeconds = spec.intervalSeconds,
            minimumDistanceMeters = spec.minimumDistanceMeters,
        )
    } else {
        invalidSpec(type)
    }

    override suspend fun start(): AppResult<Unit> = measurement.start(context)

    override suspend fun stop(): AppResult<Unit> = measurement.stop()
}

private class ActivitySource(
    private val config: MeasurementConfig,
    private val storage: MeasurementStorage,
    private val platform: ActivityRecognitionPlatform,
) : RecordingSource {
    private var measurement: ActivityRecognitionMeasurement? = null
    override val type = RecordingSourceType.ACTIVITY_RECOGNITION

    override suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.ActivityRecognition) {
            ActivityRecognitionMeasurement(spec.periodSeconds, storage, platform).also { measurement = it }.prepare(
                config.folderName,
                config.useInternalStorage,
            )
        } else {
            invalidSpec(type)
        }

    override suspend fun start(): AppResult<Unit> = measurement?.start()
        ?: invalidSpec(type)

    override suspend fun stop(): AppResult<Unit> {
        val result = measurement?.stop() ?: AppResult.success(Unit)
        measurement = null
        return result
    }
}

private class SignificantMotionSource(
    private val context: Context,
    private val config: MeasurementConfig,
    storage: MeasurementStorage,
    clock: EpochClock,
) : RecordingSource {
    private val measurement = SignificantMotion(storage, clock)
    override val type = RecordingSourceType.SIGNIFICANT_MOTION

    override suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.SignificantMotion) {
            measurement.prepare(context, config.folderName, config.useInternalStorage)
        } else {
            invalidSpec(type)
        }

    override suspend fun start(): AppResult<Unit> = measurement.start()

    override suspend fun stop(): AppResult<Unit> = measurement.stop()
}

private class SessionArtifacts(
    private val context: Context,
    private val config: MeasurementConfig,
    private val storage: MeasurementStorage,
    clock: EpochClock,
) {
    private val extraInfo = ExtraInfoHandler(storage, clock)
    private var toneGenerator: ToneGenerator? = null

    fun prepare(): AppResult<Unit> {
        val directory = storage.createMeasurementDirectory(config.folderName, config.useInternalStorage)
        return directory.onSuccess { extraInfo.start(config) }
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

    fun stop(): AppResult<Unit> {
        val releaseTone = appResult(AppErrorCode.MEASUREMENT, "Release measurement alarm") {
            toneGenerator?.release()
            toneGenerator = null
        }
        return listOf(releaseTone, extraInfo.write(context))
            .combineAppResults(AppErrorCode.MEASUREMENT, "Close measurement session artifacts")
    }

    private companion object {
        const val ALARM_DURATION_MILLIS = 1_000
    }
}

private fun invalidSpec(type: RecordingSourceType): AppResult<Nothing> = AppResult.failure(
    AppError(AppErrorCode.VALIDATION, "Prepare $type recording source"),
)
