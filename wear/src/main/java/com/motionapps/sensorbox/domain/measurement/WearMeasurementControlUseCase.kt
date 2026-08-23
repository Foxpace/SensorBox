package com.motionapps.sensorbox.domain.measurement

import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.preferences.AppPreferences
import com.motionapps.sensorbox.core.time.EpochClock
import com.motionapps.sensorservices.intent.MeasurementIntentFactory
import com.motionapps.sensorservices.intent.MeasurementLaunchRequest
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.wearoslib.protocol.WearRecordingRequest
import com.motionapps.wearoslib.protocol.WearStopReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface WearRecordingController {
    fun start(
        sessionId: String,
        request: WearRecordingRequest,
        preferences: AppPreferences,
        startAtEpochMillis: Long,
    ): AppResult<Unit>

    fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit>
}

class WearMeasurementControlUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentFactory: MeasurementIntentFactory,
    private val clock: EpochClock,
) : WearRecordingController {
    override fun start(
        sessionId: String,
        request: WearRecordingRequest,
        preferences: AppPreferences,
        startAtEpochMillis: Long,
    ): AppResult<Unit> = start(
        sessionId = sessionId,
        sensorIds = request.sensorIds.toSet(),
        includesGps = request.includesGps,
        preferences = preferences,
        folderName = request.folderName,
        startAtEpochMillis = startAtEpochMillis,
        durationMillis = request.durationMillis,
    )

    fun start(
        sensorIds: Set<Int>,
        includesGps: Boolean,
        preferences: AppPreferences,
        sessionId: String = java.util.UUID.randomUUID().toString(),
        folderName: String = intentFactory.newFolderName(),
        startAtEpochMillis: Long = clock.nowMillis(),
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
            startAtEpochMillis = startAtEpochMillis,
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
