package com.tomasrepcik.sensorbox.sensorservices.serviceController

import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ExtraInfoHandler

internal class SessionArtifacts(
    private val config: MeasurementConfig,
    private val storage: MeasurementStorage,
    clock: EpochClock,
    sensorManager: SensorManager,
) {
    private val extraInfo = ExtraInfoHandler(storage, clock, sensorManager)
    private var toneGenerator: ToneGenerator? = null

    fun start(): AppResult<Unit> = storage
        .createMeasurementDirectory(config.folderName, config.useInternalStorage)
        .onSuccess { extraInfo.start(config) }

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
        return listOf(releaseTone, extraInfo.write())
            .combineAppResults(AppErrorCode.MEASUREMENT, "Close measurement session artifacts")
    }

    private companion object {
        const val ALARM_DURATION_MILLIS = 1_000
    }
}
