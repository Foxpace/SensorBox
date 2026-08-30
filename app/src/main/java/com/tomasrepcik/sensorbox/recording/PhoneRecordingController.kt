package com.tomasrepcik.sensorbox.recording

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.failure.flatMap
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetup
import com.tomasrepcik.sensorbox.recording.setup.StartedPhoneRecording
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingIntentFactory
import com.tomasrepcik.sensorbox.recordinghost.session.RecordingService
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearStopReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PhoneRecordingController {
    fun start(sessionId: String, request: RecordingSetup): AppResult<StartedPhoneRecording>

    fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit>

    fun stopCurrent(reason: WearStopReason): AppResult<Unit>

    fun annotate(text: String, timestampMillis: Long): AppResult<Unit>
}

@Singleton
class AndroidPhoneRecordingController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentFactory: RecordingIntentFactory,
    private val documentStorage: DocumentStorage,
) : PhoneRecordingController {
    override fun start(sessionId: String, request: RecordingSetup): AppResult<StartedPhoneRecording> = documentStorage
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
        request: RecordingSetup,
    ): AppResult<StartedPhoneRecording> = appResult(
        AppErrorCode.RECORDING,
        "Start phone recording",
    ) {
        val folderName = intentFactory.newFolderName(request.customName)
        val recordingRequest = request.toRecordingRequest(sessionId, folderName)
        checkNotNull(ContextCompat.startForegroundService(context, intentFactory.create(recordingRequest))) {
            "Recording foreground service is not registered"
        }
        StartedPhoneRecording(sessionId, folderName, recordingRequest.durationMillis)
    }

    override fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit> = stopService("Stop phone recording")

    override fun stopCurrent(reason: WearStopReason): AppResult<Unit> = stopService("Stop current phone recording")

    override fun annotate(text: String, timestampMillis: Long): AppResult<Unit> = appResult(
        AppErrorCode.RECORDING,
        "Request recording annotation",
    ) {
        val intent = Intent(context, RecordingService::class.java)
            .setAction(RecordingService.ACTION_ANNOTATE)
            .putExtra(RecordingService.ANNOTATION_TIME, timestampMillis)
            .putExtra(RecordingService.ANNOTATION_TEXT, text)
        context.startService(intent)
    }

    private fun stopService(operation: String): AppResult<Unit> = appResult(AppErrorCode.RECORDING, operation) {
        val stopIntent = Intent(context, RecordingService::class.java)
            .setAction(RecordingService.ACTION_STOP_RECORDING)
        context.startService(stopIntent)
    }
}
