package com.motionapps.sensorbox.domain.measurement

import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.suspendFlatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorbox.core.storage.NativeDocumentStorage
import com.motionapps.sensorservices.intent.MeasurementIntentFactory
import com.motionapps.sensorservices.intent.MeasurementLaunchRequest
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.motionapps.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MeasurementControlUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentFactory: MeasurementIntentFactory,
    private val sendWearMessage: SendWearMessageUseCase,
) {
    suspend fun start(request: MeasurementRequest): Result<Unit> =
        NativeDocumentStorage.hasAppDirectory(context, APP_DIRECTORY).suspendFlatMap { storageReady ->
            if (!storageReady) {
                return@suspendFlatMap Result.failure(
                    AppError(AppError.Kind.STORAGE, "Storage directory is not configured"),
                )
            }
            val launchRequest = request.toLaunchRequest()
            val remoteStart = if (request.wearSensorIds.isNotEmpty() || request.wearIncludesGps) {
                WearCommandCodec.encode(
                    WearCommand.StartMeasurement(
                        folderName = launchRequest.folderName,
                        sensorIds = request.wearSensorIds.toList(),
                        includesGps = request.wearIncludesGps,
                        startAtEpochMillis = launchRequest.startAtEpochMillis,
                        durationMillis = launchRequest.durationMillis,
                        measurementType = launchRequest.measurementType,
                    ),
                ).suspendFlatMap { payload ->
                    sendWearMessage(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, payload)
                }
            } else {
                Result.success(Unit)
            }
            remoteStart.flatMap {
                appResult(AppError.Kind.MEASUREMENT, "Launch measurement service") {
                    ContextCompat.startForegroundService(context, intentFactory.create(launchRequest))
                    Unit
                }
            }
        }.withAppError(AppError.Kind.MEASUREMENT, "Request measurement start")

    fun stop(): Result<Unit> = appResult(AppError.Kind.MEASUREMENT, "Request measurement stop") {
        val stopIntent = Intent(context, MeasurementService::class.java)
            .setAction(MeasurementService.ACTION_STOP)
        context.startService(stopIntent)
    }

    fun annotate(text: String, timestampMillis: Long = System.currentTimeMillis()): Result<Unit> = appResult(
        AppError.Kind.MEASUREMENT,
        "Request measurement annotation",
    ) {
        val intent = Intent(context, MeasurementService::class.java)
            .setAction(MeasurementService.ACTION_ANNOTATE)
            .putExtra(MeasurementService.ANNOTATION_TIME, timestampMillis)
            .putExtra(MeasurementService.ANNOTATION_TEXT, text)
        context.startService(intent)
    }

    private fun MeasurementRequest.toLaunchRequest(): MeasurementLaunchRequest = MeasurementLaunchRequest(
        folderName = intentFactory.newFolderName(customName, measurementType),
        useInternalStorage = false,
        sensorIds = sensorIds,
        sensorSamplingPeriod = samplingPeriod(samplingPeriodIndex),
        includesGps = includesGps,
        stopOnLowBattery = stopOnLowBattery,
        useWakeLock = useWakeLock,
        gpsIntervalSeconds = gpsIntervalSeconds,
        gpsMinDistanceMeters = gpsMinDistanceMeters,
        measurementType = measurementType,
        startAtEpochMillis = System.currentTimeMillis() + delaySeconds.coerceAtLeast(0) * 1_000L +
            if (wearSensorIds.isNotEmpty() || wearIncludesGps) WEAR_START_LEAD_MILLIS else 0L,
        durationMillis = durationSeconds.coerceAtLeast(0) * 1_000L,
        notes = notes,
        alarmOffsetsSeconds = alarmOffsetsSeconds,
        activityRecognition = activityRecognition,
        activityRecognitionPeriodSeconds = activityRecognitionPeriodSeconds,
        significantMotion = significantMotion,
        controlsWearMeasurement = wearSensorIds.isNotEmpty() || wearIncludesGps,
    )

    private fun samplingPeriod(index: Int): Int = SENSOR_PERIODS.getOrElse(index) {
        SensorManager.SENSOR_DELAY_FASTEST
    }

    private companion object {
        const val APP_DIRECTORY = "SensorBox"
        val SENSOR_PERIODS = intArrayOf(
            SensorManager.SENSOR_DELAY_FASTEST,
            SensorManager.SENSOR_DELAY_GAME,
            SensorManager.SENSOR_DELAY_UI,
            SensorManager.SENSOR_DELAY_NORMAL,
        )
        const val WEAR_START_LEAD_MILLIS = 1_000L
    }
}
