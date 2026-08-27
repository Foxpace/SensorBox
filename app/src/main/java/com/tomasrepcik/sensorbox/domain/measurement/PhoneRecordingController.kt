package com.tomasrepcik.sensorbox.domain.measurement

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementIntentFactory
import com.tomasrepcik.sensorbox.sensorservices.services.MeasurementService
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearStopReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PhoneRecordingController {
    fun start(sessionId: String, request: MeasurementRequest): AppResult<StartedPhoneRecording>

    fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit>

    fun stopCurrent(reason: WearStopReason): AppResult<Unit>

    fun annotate(text: String, timestampMillis: Long): AppResult<Unit>
}

@Singleton
class AndroidPhoneRecordingController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentFactory: MeasurementIntentFactory,
    private val documentStorage: DocumentStorage,
) : PhoneRecordingController {
    override fun start(sessionId: String, request: MeasurementRequest): AppResult<StartedPhoneRecording> =
        documentStorage
            .hasConfiguredDirectory()
            .flatMap { storageReady ->
                if (!storageReady) {
                    AppResult.failure(AppError(AppErrorCode.STORAGE, "Start phone recording storage"))
                } else {
                    startForegroundRecording(sessionId, request)
                }
            }

    private fun startForegroundRecording(
        sessionId: String,
        request: MeasurementRequest,
    ): AppResult<StartedPhoneRecording> = appResult(
        AppErrorCode.MEASUREMENT,
        "Start phone recording",
    ) {
        val folderName = intentFactory.newFolderName(request.customName)
        val launchRequest = request.toLaunchRequest(sessionId, folderName)
        ContextCompat.startForegroundService(context, intentFactory.create(launchRequest))
        StartedPhoneRecording(sessionId, folderName, launchRequest.durationMillis)
    }

    override fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit> = stopService("Stop phone recording")

    override fun stopCurrent(reason: WearStopReason): AppResult<Unit> = stopService("Stop current phone recording")

    override fun annotate(text: String, timestampMillis: Long): AppResult<Unit> = appResult(
        AppErrorCode.MEASUREMENT,
        "Request measurement annotation",
    ) {
        val intent = Intent(context, MeasurementService::class.java)
            .setAction(MeasurementService.ACTION_ANNOTATE)
            .putExtra(MeasurementService.ANNOTATION_TIME, timestampMillis)
            .putExtra(MeasurementService.ANNOTATION_TEXT, text)
        context.startService(intent)
    }

    private fun stopService(operation: String): AppResult<Unit> = appResult(AppErrorCode.MEASUREMENT, operation) {
        val stopIntent = Intent(context, MeasurementService::class.java)
            .setAction(MeasurementService.ACTION_STOP)
        context.startService(stopIntent)
    }
}
