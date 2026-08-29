package com.tomasrepcik.sensorbox.sensorservices.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingStopReason
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStopReason
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {
    @Inject
    lateinit var sessionStore: RecordingSessionStore

    @Inject
    internal lateinit var sessionFactory: RecordingHostSessionFactory

    @Inject
    lateinit var epochClock: EpochClock

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var hostSession: RecordingHostSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RECORDING -> hostSession?.requestStop(RecordingStopReason.USER_REQUEST)

            ACTION_ANNOTATE -> hostSession?.annotate(
                intent.getLongExtra(ANNOTATION_TIME, epochClock.nowMillis()),
                intent.getStringExtra(ANNOTATION_TEXT).orEmpty(),
            )

            else -> intent?.takeIf { hostSession == null }?.let(::startRecordingHost)
        }
        return START_NOT_STICKY
    }

    private fun startRecordingHost(intent: Intent) {
        appResult(AppErrorCode.RECORDING, "Start recording foreground host") {
            val request = RecordingRequest.from(intent)
            require(request.sessionId.isNotBlank()) { "Recording session ID is missing" }
            sessionFactory.create(this, request, serviceScope).also { session ->
                hostSession = session
                serviceScope.launch { session.start() }
            }
        }.onFailure { error ->
            sessionStore.markIdle()
            sessionStore.publishStopped(
                sessionId = "",
                reason = RecordingSessionStopReason.SOURCE_FAILURE,
                result = AppResult.failure(error),
            )
            stopSelf()
        }
    }

    override fun onDestroy() {
        hostSession?.destroy()
        hostSession = null
        serviceScope.cancel()
        super.onDestroy()
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
