package com.motionapps.sensorservices.serviceController

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorservices.handlers.GPSHandler
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.handlers.measurements.ActivityRecognitionMeasurement
import com.motionapps.sensorservices.handlers.measurements.ExtraInfoHandler
import com.motionapps.sensorservices.handlers.measurements.GPSMeasurement
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface
import com.motionapps.sensorservices.handlers.measurements.SensorMeasurement
import com.motionapps.sensorservices.handlers.measurements.SignificantMotion
import com.motionapps.sensorservices.services.MeasurementService

class ServiceController {
    private val sensorMeasurement = SensorMeasurement()
    private val gpsMeasurement = GPSMeasurement(GPSHandler())
    private var activeConfig: MeasurementConfig? = null
    private val extraInfo = ExtraInfoHandler()
    private var activityRecognition: ActivityRecognitionMeasurement? = null
    private val significantMotion = SignificantMotion()
    private var toneGenerator: ToneGenerator? = null

    fun start(context: Context, config: MeasurementConfig): Result<Unit> {
        var result = createMeasurementDirectory(context, config).flatMap {
            appResult(AppError.Kind.MEASUREMENT, "Initialize measurement session") {
                activeConfig = config
                extraInfo.start(config)
            }
        }
        if (config.sensorIds.isNotEmpty()) result = result.flatMap { startSensors(context, config) }
        if (config.includesGps) result = result.flatMap { startGps(context, config) }
        if (config.activityRecognition) result = result.flatMap { startActivityRecognition(context, config) }
        if (config.significantMotion) result = result.flatMap { startSignificantMotion(context, config) }
        return result.withAppError(AppError.Kind.MEASUREMENT, "Start measurement controller")
    }

    suspend fun stop(context: Context): Result<Unit> {
        val config = activeConfig ?: return Result.success(Unit)
        val results = mutableListOf<Result<*>>()
        if (config.sensorIds.isNotEmpty()) results += sensorMeasurement.onDestroyMeasurement(context)
        if (config.includesGps) results += gpsMeasurement.onDestroyMeasurement(context)
        activityRecognition?.let { results += it.onDestroyMeasurement(context) }
        activityRecognition = null
        if (config.significantMotion) results += significantMotion.onDestroyMeasurement(context)
        results += appResult(AppError.Kind.MEASUREMENT, "Release alarm") {
            toneGenerator?.release()
            Unit
        }
        toneGenerator = null
        results += extraInfo.write(context)
        activeConfig = null
        return results.combineAppResults(AppError.Kind.MEASUREMENT, "Stop measurement controller")
    }

    fun annotate(timestampMillis: Long, text: String): Result<Unit> = appResult(
        AppError.Kind.MEASUREMENT,
        "Add measurement annotation",
    ) {
        extraInfo.annotate(timestampMillis, text)
    }

    fun playAlarm(): Result<Unit> = appResult(AppError.Kind.MEASUREMENT, "Play measurement alarm") {
        extraInfo.alarmTriggered()
        val tone = toneGenerator ?: ToneGenerator(AudioManager.STREAM_ALARM, 100).also { toneGenerator = it }
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, ALARM_DURATION_MILLIS)
    }

    private fun createMeasurementDirectory(context: Context, config: MeasurementConfig): Result<Unit> =
        if (config.useInternalStorage) {
            StorageHandler.createInternalStorageMeasurementFolder(context, config.folderName)
        } else {
            StorageHandler.createFolderMeasurement(context, config.folderName)
        }

    private fun startSensors(context: Context, config: MeasurementConfig): Result<Unit> {
        val params = baseParams(config).apply {
            putIntArray(MeasurementInterface.SENSOR_ID, config.sensorIds)
            putInt(MeasurementInterface.SENSOR_SPEED, config.sensorSamplingPeriod)
        }
        return sensorMeasurement.initMeasurement(context, params)
            .flatMap { sensorMeasurement.startMeasurement(context) }
            .withAppError(AppError.Kind.MEASUREMENT, "Start configured sensors")
    }

    private fun startGps(context: Context, config: MeasurementConfig): Result<Unit> {
        val params = baseParams(config).apply {
            putInt(MeasurementService.GPS_INTERVAL_SECONDS, config.gpsIntervalSeconds)
            putInt(MeasurementService.GPS_DISTANCE_METERS, config.gpsMinDistanceMeters)
        }
        return gpsMeasurement.initMeasurement(context, params)
            .flatMap { gpsMeasurement.startMeasurement(context) }
            .withAppError(AppError.Kind.MEASUREMENT, "Start configured GPS")
    }

    private fun startActivityRecognition(context: Context, config: MeasurementConfig): Result<Unit> {
        val handler = ActivityRecognitionMeasurement(config.activityRecognitionPeriodSeconds)
        return handler.initMeasurement(context, baseParams(config))
            .flatMap { handler.startMeasurement(context) }
            .onSuccess { activityRecognition = handler }
            .withAppError(AppError.Kind.MEASUREMENT, "Start configured activity recognition")
    }

    private fun startSignificantMotion(context: Context, config: MeasurementConfig): Result<Unit> =
        significantMotion.initMeasurement(context, baseParams(config))
            .flatMap { significantMotion.startMeasurement(context) }
            .withAppError(AppError.Kind.MEASUREMENT, "Start configured significant motion")

    private fun baseParams(config: MeasurementConfig) = Bundle().apply {
        putString(MeasurementInterface.FOLDER_NAME, config.folderName)
        putBoolean(MeasurementInterface.INTERNAL_STORAGE, config.useInternalStorage)
    }

    private companion object {
        const val ALARM_DURATION_MILLIS = 1_000
    }
}
