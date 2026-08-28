package com.tomasrepcik.sensorbox.sensorservices.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingEvent
import com.tomasrepcik.sensorbox.recording.RecordingStopReason
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest
import com.tomasrepcik.sensorbox.sensorservices.serviceController.ServiceController
import com.tomasrepcik.sensorbox.sensorservices.serviceController.ServiceControllerFactory
import com.tomasrepcik.sensorbox.sensorservices.session.RecordingSessionState
import com.tomasrepcik.sensorbox.sensorservices.session.RecordingSessionStopReason
import com.tomasrepcik.sensorbox.sensorservices.session.RecordingSessionStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {
    @Inject
    lateinit var sessionStore: RecordingSessionStore

    @Inject
    internal lateinit var controllerFactory: ServiceControllerFactory

    @Inject
    lateinit var epochClock: EpochClock

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: ServiceController? = null
    private var activeRequest: RecordingRequest? = null
    private var eventJob: Job? = null
    private var isFinishing = false
    private val hostResources by lazy {
        RecordingHostResources(
            service = this,
            scope = serviceScope,
            requestStop = ::requestStop,
            playAlarm = { controller?.playAlarm() },
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RECORDING -> requestStop(RecordingStopReason.USER_REQUEST)

            ACTION_ANNOTATE -> controller?.annotate(
                intent.getLongExtra(ANNOTATION_TIME, epochClock.nowMillis()),
                intent.getStringExtra(ANNOTATION_TEXT).orEmpty(),
            )

            else -> intent?.takeIf { activeRequest == null }?.let(::startRecordingHost)
        }
        return START_NOT_STICKY
    }

    private fun startRecordingHost(intent: Intent) {
        appResult(AppErrorCode.RECORDING, "Start recording foreground host") {
            val request = RecordingRequest.from(intent)
            require(request.sessionId.isNotBlank()) { "Recording session ID is missing" }
            activeRequest = request
            isFinishing = false
            hostResources.start(request)
            val serviceController = controllerFactory.create(this, request, serviceScope)
            controller = serviceController
            observeEngine(serviceController)
            serviceScope.launch {
                val result = serviceController.start()
                if (result.isFailure) {
                    finishRecording(
                        RecordingSessionStopReason.SOURCE_FAILURE,
                        result,
                    )
                }
            }
        }.onFailure { error ->
            serviceScope.launch {
                finishRecording(
                    RecordingSessionStopReason.SOURCE_FAILURE,
                    AppResult.failure(error),
                )
            }
        }
    }

    private fun observeEngine(serviceController: ServiceController) {
        eventJob?.cancel()
        eventJob = serviceScope.launch {
            serviceController.events.collect { event ->
                when (event) {
                    is RecordingEvent.RecordingStarted -> onRecordingStarted(event)

                    is RecordingEvent.RecordingStopped -> finishRecording(
                        event.reason.toRecordingSessionReason(),
                        event.result,
                    )
                }
            }
        }
    }

    private fun onRecordingStarted(event: RecordingEvent.RecordingStarted) {
        val request = activeRequest ?: return
        sessionStore.markRunning(
            RecordingSessionState.Running(
                sessionId = event.sessionId.value,
                folderName = request.folderName,
                startedAtElapsedRealtime = SystemClock.elapsedRealtime(),
                sensorIds = request.sensorIds.toList(),
                includesGps = request.includesGps,
            ),
        )
        hostResources.scheduleAlarms(request.alarmOffsetsSeconds)
    }

    private fun requestStop(reason: RecordingStopReason) {
        if (isFinishing) return
        sessionStore.markStopping()
        serviceScope.launch {
            controller?.stop(reason)
        }
    }

    private suspend fun finishRecording(reason: RecordingSessionStopReason, engineResult: AppResult<Unit>) {
        if (isFinishing) return
        isFinishing = true
        val sessionId = activeRequest?.sessionId.orEmpty()
        val hostResult = finishHost()
        val result = listOf(engineResult, hostResult)
            .combineAppResults(AppErrorCode.RECORDING, "Finish recording foreground host")
        sessionStore.publishStopped(sessionId, reason, result)
        eventJob?.cancel()
        eventJob = null
    }

    private fun finishHost(): AppResult<Unit> {
        val results = mutableListOf<AppResult<*>>()
        results += hostResources.release()
        activeRequest = null
        controller = null
        results += appResult(AppErrorCode.RECORDING, "Publish idle recording state") {
            sessionStore.markIdle()
        }
        results += appResult(AppErrorCode.RECORDING, "Remove recording notification") {
            hostResources.removeNotification()
        }
        results += appResult(AppErrorCode.RECORDING, "Stop recording service instance") {
            stopSelf()
        }
        return results.combineAppResults(AppErrorCode.RECORDING, "Finish recording host resources")
    }

    override fun onDestroy() {
        val request = activeRequest
        if (request != null && !isFinishing) {
            isFinishing = true
            val cleanup = runBlocking(Dispatchers.IO) {
                controller?.stop(RecordingStopReason.PLATFORM_DESTROYED) ?: AppResult.success(Unit)
            }
            val resources = hostResources.release()
            sessionStore.markIdle()
            sessionStore.publishStopped(
                request.sessionId,
                RecordingSessionStopReason.SERVICE_DESTROYED,
                listOf(cleanup, resources).combineAppResults(
                    AppErrorCode.RECORDING,
                    "Destroy recording foreground host",
                ),
            )
        }
        eventJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun RecordingStopReason.toRecordingSessionReason(): RecordingSessionStopReason = when (this) {
        RecordingStopReason.USER_REQUEST -> RecordingSessionStopReason.USER_REQUEST
        RecordingStopReason.DURATION_EXPIRED -> RecordingSessionStopReason.DURATION_EXPIRED
        RecordingStopReason.LOW_BATTERY -> RecordingSessionStopReason.LOW_BATTERY
        RecordingStopReason.SOURCE_FAILURE -> RecordingSessionStopReason.SOURCE_FAILURE
        RecordingStopReason.PLATFORM_DESTROYED -> RecordingSessionStopReason.SERVICE_DESTROYED
    }

    companion object {
        const val SESSION_ID = "SESSION_ID"
        const val FOLDER_NAME = "FOLDER_NAME"
        const val INTERNAL_STORAGE = "INTERNAL_STORAGE"
        const val ANDROID_SENSORS = "ANDROID_SENSORS"
        const val ANDROID_SENSORS_SPEED = "ANDROID_SENSORS_SPEED"
        const val GPS = "GPS_MEASURE"
        const val STOP_ON_LOW_BATTERY = "STOP_ON_LOW_BATTERY"
        const val USE_WAKE_LOCK = "USE_WAKE_LOCK"
        const val GPS_INTERVAL_SECONDS = "GPS_INTERVAL_SECONDS"
        const val GPS_DISTANCE_METERS = "GPS_DISTANCE_METERS"
        const val DURATION_MILLIS = "DURATION_MILLIS"
        const val NOTES = "NOTES"
        const val ALARM_OFFSETS_SECONDS = "ALARM_OFFSETS_SECONDS"
        const val ACTIVITY_RECOGNITION = "ACTIVITY_RECOGNITION"
        const val ACTIVITY_RECOGNITION_PERIOD_SECONDS = "ACTIVITY_RECOGNITION_PERIOD_SECONDS"
        const val SIGNIFICANT_MOTION = "SIGNIFICANT_MOTION"
        const val ACTION_ANNOTATE = "com.tomasrepcik.sensorbox.action.ANNOTATE_MEASUREMENT"
        const val ANNOTATION_TIME = "ANNOTATION_TIME"
        const val ANNOTATION_TEXT = "ANNOTATION_TEXT"
        const val ACTION_STOP_RECORDING = "com.tomasrepcik.sensorbox.action.STOP_RECORDING"
    }
}
