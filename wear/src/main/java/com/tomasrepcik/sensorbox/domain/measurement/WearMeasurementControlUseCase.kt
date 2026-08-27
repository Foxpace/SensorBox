package com.tomasrepcik.sensorbox.domain.measurement

import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementIntentFactory
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementLaunchRequest
import com.tomasrepcik.sensorbox.sensorservices.services.MeasurementService
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearStopReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface WearRecordingControlUseCase {
    fun start(sessionId: String, request: WearRecordingRequest, preferences: AppPreferences): AppResult<Unit>

    fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit>
}

class WearMeasurementControlUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentFactory: MeasurementIntentFactory,
) : WearRecordingControlUseCase {
    override fun start(
        sessionId: String,
        request: WearRecordingRequest,
        preferences: AppPreferences,
    ): AppResult<Unit> = start(
        sessionId = sessionId,
        sensorIds = request.sensorIds.toSet(),
        includesGps = request.includesGps,
        preferences = preferences,
        folderName = request.folderName,
        durationMillis = request.durationMillis,
    )

    fun start(
        sensorIds: Set<Int>,
        includesGps: Boolean,
        preferences: AppPreferences,
        sessionId: String = java.util.UUID.randomUUID().toString(),
        folderName: String = intentFactory.newFolderName(),
        durationMillis: Long = 0L,
    ): AppResult<Unit> = appResult(AppErrorCode.MEASUREMENT, "Request Wear measurement start") {
        val request = MeasurementLaunchRequest(
            sessionId = sessionId,
            folderName = folderName,
            useInternalStorage = true,
            sensorIds = sensorIds,
            sensorSamplingPeriod = samplingPeriod(preferences.recording.sensorSamplingPeriod),
            includesGps = includesGps,
            stopOnLowBattery = preferences.recording.restrictMeasurementOnLowBattery,
            useWakeLock = preferences.recording.useWakeLock,
            gpsIntervalSeconds = preferences.recording.gpsIntervalSeconds,
            gpsMinDistanceMeters = preferences.recording.gpsMinDistanceMeters,
            durationMillis = durationMillis,
        )
        ContextCompat.startForegroundService(context, intentFactory.create(request))
    }

    fun stop(): AppResult<Unit> = appResult(AppErrorCode.MEASUREMENT, "Request Wear measurement stop") {
        val intent = Intent(context, MeasurementService::class.java)
            .setAction(MeasurementService.ACTION_STOP)
        context.startService(intent)
    }

    override fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit> = stop()

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
