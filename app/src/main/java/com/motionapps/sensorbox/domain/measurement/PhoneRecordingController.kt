package com.motionapps.sensorbox.domain.measurement

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.storage.DocumentStorage
import com.motionapps.sensorservices.intent.MeasurementIntentFactory
import com.motionapps.sensorservices.intent.MeasurementLaunchRequest
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.wearoslib.protocol.WearStopReason
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PreparedPhoneRecording(val sessionId: String, val launchRequest: MeasurementLaunchRequest)

interface PhoneRecordingController {
    suspend fun prepare(sessionId: String, request: MeasurementRequest): AppResult<PreparedPhoneRecording>

    fun commit(prepared: PreparedPhoneRecording, startAtEpochMillis: Long): AppResult<Unit>

    fun abort(sessionId: String): AppResult<Unit>

    fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit>

    fun stopAny(reason: WearStopReason): AppResult<Unit>

    fun annotate(text: String, timestampMillis: Long): AppResult<Unit>
}

@Singleton
class AndroidPhoneRecordingController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val intentFactory: MeasurementIntentFactory,
    private val documentStorage: DocumentStorage,
) : PhoneRecordingController {
    private val committedSessions = mutableSetOf<String>()

    override suspend fun prepare(sessionId: String, request: MeasurementRequest): AppResult<PreparedPhoneRecording> =
        documentStorage
            .hasConfiguredDirectory()
            .flatMap { storageReady ->
                if (!storageReady) {
                    AppResult.failure(AppError(AppErrorCode.STORAGE, "Prepare phone recording storage"))
                } else {
                    AppResult.success(
                        PreparedPhoneRecording(
                            sessionId = sessionId,
                            launchRequest = request.toLaunchRequest(
                                sessionId,
                                intentFactory.newFolderName(request.customName),
                            ),
                        ),
                    )
                }
            }

    override fun commit(prepared: PreparedPhoneRecording, startAtEpochMillis: Long): AppResult<Unit> = appResult(
        AppErrorCode.MEASUREMENT,
        "Commit phone recording",
    ) {
        val launchRequest = prepared.launchRequest.copy(startAtEpochMillis = startAtEpochMillis)
        ContextCompat.startForegroundService(context, intentFactory.create(launchRequest))
        synchronized(committedSessions) { committedSessions += prepared.sessionId }
    }

    override fun abort(sessionId: String): AppResult<Unit> {
        val wasCommitted = synchronized(committedSessions) { committedSessions.remove(sessionId) }
        return if (wasCommitted) stopService("Abort phone recording") else AppResult.success(Unit)
    }

    override fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit> {
        synchronized(committedSessions) { committedSessions.remove(sessionId) }
        return stopService("Stop phone recording")
    }

    override fun stopAny(reason: WearStopReason): AppResult<Unit> = stopService("Stop phone recording")

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
