package com.motionapps.sensorservices.serviceController

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.recording.RecordingSource
import com.motionapps.sensorbox.recording.RecordingSourceSpec
import com.motionapps.sensorbox.recording.RecordingSourceType
import com.motionapps.sensorservices.handlers.GPSHandler
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.handlers.measurements.ActivityRecognitionMeasurement
import com.motionapps.sensorservices.handlers.measurements.ExtraInfoHandler
import com.motionapps.sensorservices.handlers.measurements.GPSMeasurement
import com.motionapps.sensorservices.handlers.measurements.SensorMeasurement
import com.motionapps.sensorservices.handlers.measurements.SignificantMotion

class AndroidRecordingSources(private val context: Context, private val config: MeasurementConfig) {
    private val artifacts = SessionArtifacts(context, config)

    val sources: List<RecordingSource> = listOf(
        SessionSource(artifacts),
        SensorSource(context, config),
        GpsSource(context, config),
        ActivitySource(context, config),
        SignificantMotionSource(context, config),
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

private class SensorSource(private val context: Context, private val config: MeasurementConfig) : RecordingSource {
    private val measurement = SensorMeasurement()
    override val type = RecordingSourceType.SENSOR

    override suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.Sensors) {
            measurement.prepare(
                context = context,
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

private class GpsSource(private val context: Context, private val config: MeasurementConfig) : RecordingSource {
    private val measurement = GPSMeasurement(GPSHandler())
    override val type = RecordingSourceType.GPS

    override suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit> = if (spec is RecordingSourceSpec.Gps) {
        measurement.prepare(
            context = context,
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

private class ActivitySource(private val context: Context, private val config: MeasurementConfig) : RecordingSource {
    private var measurement: ActivityRecognitionMeasurement? = null
    override val type = RecordingSourceType.ACTIVITY_RECOGNITION

    override suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.ActivityRecognition) {
            ActivityRecognitionMeasurement(spec.periodSeconds).also { measurement = it }.prepare(
                context,
                config.folderName,
                config.useInternalStorage,
            )
        } else {
            invalidSpec(type)
        }

    override suspend fun start(): AppResult<Unit> = measurement?.start(context)
        ?: invalidSpec(type)

    override suspend fun stop(): AppResult<Unit> {
        val result = measurement?.stop(context) ?: AppResult.success(Unit)
        measurement = null
        return result
    }
}

private class SignificantMotionSource(private val context: Context, private val config: MeasurementConfig) :
    RecordingSource {
    private val measurement = SignificantMotion()
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

private class SessionArtifacts(private val context: Context, private val config: MeasurementConfig) {
    private val extraInfo = ExtraInfoHandler()
    private var toneGenerator: ToneGenerator? = null

    fun prepare(): AppResult<Unit> {
        val directory = if (config.useInternalStorage) {
            StorageHandler.createInternalStorageMeasurementFolder(context, config.folderName)
        } else {
            StorageHandler.createFolderMeasurement(context, config.folderName)
        }
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
