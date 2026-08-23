package com.motionapps.sensorbox.domain.measurement

import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.preferences.AppPreferences
import com.motionapps.sensorservices.intent.MeasurementIntentFactory
import com.motionapps.sensorservices.intent.MeasurementLaunchRequest
import com.motionapps.sensorservices.services.MeasurementService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WearMeasurementControlUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentFactory: MeasurementIntentFactory,
) {
    fun start(
        sensorIds: Set<Int>,
        includesGps: Boolean,
        preferences: AppPreferences,
        folderName: String = intentFactory.newFolderName(),
        startAtEpochMillis: Long = System.currentTimeMillis(),
        durationMillis: Long = 0L,
        measurementType: String = "ENDLESS",
    ): Result<Unit> = appResult(AppError.Kind.MEASUREMENT, "Request Wear measurement start") {
        val request = MeasurementLaunchRequest(
            folderName = folderName,
            useInternalStorage = true,
            sensorIds = sensorIds,
            sensorSamplingPeriod = samplingPeriod(preferences.sensorSamplingPeriod),
            includesGps = includesGps,
            stopOnLowBattery = preferences.restrictMeasurementOnLowBattery,
            useWakeLock = preferences.useWakeLock,
            gpsIntervalSeconds = preferences.gpsIntervalSeconds,
            gpsMinDistanceMeters = preferences.gpsMinDistanceMeters,
            measurementType = measurementType,
            startAtEpochMillis = startAtEpochMillis,
            durationMillis = durationMillis,
        )
        ContextCompat.startForegroundService(context, intentFactory.create(request))
    }

    fun stop(): Result<Unit> = appResult(AppError.Kind.MEASUREMENT, "Request Wear measurement stop") {
        val intent = Intent(context, MeasurementService::class.java)
            .setAction(MeasurementService.ACTION_STOP)
        context.startService(intent)
    }

    private fun samplingPeriod(index: Int): Int = SENSOR_PERIODS.getOrElse(index) {
        SensorManager.SENSOR_DELAY_FASTEST
    }

    private companion object {
        val SENSOR_PERIODS = intArrayOf(
            SensorManager.SENSOR_DELAY_FASTEST,
            SensorManager.SENSOR_DELAY_GAME,
            SensorManager.SENSOR_DELAY_UI,
            SensorManager.SENSOR_DELAY_NORMAL,
        )
    }
}
