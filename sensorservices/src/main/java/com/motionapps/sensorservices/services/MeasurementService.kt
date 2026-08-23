package com.motionapps.sensorservices.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.DiagnosticLogger
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.time.EpochClock
import com.motionapps.sensorbox.recording.RecordingEvent
import com.motionapps.sensorbox.recording.RecordingStopReason
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.serviceController.MeasurementConfig
import com.motionapps.sensorservices.serviceController.ServiceController
import com.motionapps.sensorservices.session.MeasurementSessionState
import com.motionapps.sensorservices.session.MeasurementSessionStore
import com.motionapps.sensorservices.session.MeasurementStopReason
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MeasurementService : Service() {
    @Inject
    lateinit var sessionStore: MeasurementSessionStore

    @Inject
    internal lateinit var storageHandler: StorageHandler

    @Inject
    lateinit var diagnosticLogger: DiagnosticLogger

    @Inject
    lateinit var epochClock: EpochClock

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: ServiceController? = null
    private var activeConfig: MeasurementConfig? = null
    private var eventJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var batteryReceiverRegistered = false
    private var alarmJobs: List<Job> = emptyList()
    private var isFinishing = false

    private val lowBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_LOW) requestStop(RecordingStopReason.LOW_BATTERY)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> requestStop(RecordingStopReason.USER_REQUEST)

            ACTION_ANNOTATE -> controller?.annotate(
                intent.getLongExtra(ANNOTATION_TIME, epochClock.nowMillis()),
                intent.getStringExtra(ANNOTATION_TEXT).orEmpty(),
            )

            else -> intent?.takeIf { activeConfig == null }?.let(::startRecordingHost)
        }
        return START_NOT_STICKY
    }

    private fun startRecordingHost(intent: Intent) {
        appResult(AppErrorCode.MEASUREMENT, "Start recording foreground host") {
            val config = MeasurementConfig.from(intent, epochClock)
            require(config.sessionId.isNotBlank()) { "Recording session ID is missing" }
            activeConfig = config
            isFinishing = false
            promoteToForeground(config)
            configureRuntimeResources(config)
            val serviceController = ServiceController(
                context = this,
                config = config,
                scope = serviceScope,
                storage = storageHandler,
                diagnosticLogger = diagnosticLogger,
                clock = epochClock,
            )
            controller = serviceController
            observeEngine(serviceController)
            serviceScope.launch {
                val result = serviceController.prepareAndCommit()
                if (result.isFailure && !isFinishing) {
                    finishRecording(MeasurementStopReason.SOURCE_FAILURE, result)
                }
            }
        }.onFailure { error ->
            serviceScope.launch {
                finishRecording(
                    MeasurementStopReason.SOURCE_FAILURE,
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

                    is RecordingEvent.RecordingStartRejected -> finishRecording(
                        MeasurementStopReason.SOURCE_FAILURE,
                        AppResult.failure(event.error),
                    )

                    is RecordingEvent.RecordingStopped -> finishRecording(
                        event.reason.toMeasurementReason(),
                        event.result,
                    )
                }
            }
        }
    }

    private fun onRecordingStarted(event: RecordingEvent.RecordingStarted) {
        val config = activeConfig ?: return
        sessionStore.markRunning(
            MeasurementSessionState.Running(
                sessionId = event.sessionId.value,
                folderName = config.folderName,
                startedAtElapsedRealtime = SystemClock.elapsedRealtime(),
                sensorIds = config.sensorIds.toList(),
                includesGps = config.includesGps,
            ),
        )
        scheduleAlarms(config)
    }

    private fun requestStop(reason: RecordingStopReason) {
        if (isFinishing) return
        sessionStore.markStopping()
        serviceScope.launch {
            val result = controller?.stop(reason) ?: AppResult.success(Unit)
            if (result.isFailure && !isFinishing) finishRecording(reason.toMeasurementReason(), result)
        }
    }

    private suspend fun finishRecording(reason: MeasurementStopReason, engineResult: AppResult<Unit>) {
        if (isFinishing) return
        isFinishing = true
        val sessionId = activeConfig?.sessionId.orEmpty()
        val hostResult = finishHost()
        val result = listOf(engineResult, hostResult)
            .combineAppResults(AppErrorCode.MEASUREMENT, "Finish recording foreground host")
        sessionStore.publishStopped(sessionId, reason, result)
        eventJob?.cancel()
        eventJob = null
    }

    private fun finishHost(): AppResult<Unit> {
        val results = mutableListOf<AppResult<*>>()
        results += releaseRuntimeResources()
        activeConfig = null
        controller = null
        results += appResult(AppErrorCode.MEASUREMENT, "Publish idle measurement state") {
            sessionStore.markIdle()
        }
        results += appResult(AppErrorCode.MEASUREMENT, "Remove measurement notification") {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        results += appResult(AppErrorCode.MEASUREMENT, "Stop measurement service instance") {
            stopSelf()
        }
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Finish recording host resources")
    }

    private fun scheduleAlarms(config: MeasurementConfig) {
        alarmJobs = config.alarmOffsetsSeconds.distinct().sorted().map { seconds ->
            serviceScope.launch {
                delay(seconds * 1_000L)
                controller?.playAlarm()
            }
        }
    }

    private fun promoteToForeground(config: MeasurementConfig) {
        val notification = Notify.createRecordingNotification(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, foregroundTypes(config))
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun foregroundTypes(config: MeasurementConfig): Int = if (config.includesGps) {
        FOREGROUND_SERVICE_TYPE_HEALTH or FOREGROUND_SERVICE_TYPE_LOCATION
    } else {
        FOREGROUND_SERVICE_TYPE_HEALTH
    }

    private fun configureRuntimeResources(config: MeasurementConfig) {
        if (config.useWakeLock) acquireWakeLock()
        if (config.stopOnLowBattery) registerLowBatteryReceiver()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply { acquire() }
    }

    private fun registerLowBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_LOW)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(lowBatteryReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(lowBatteryReceiver, filter)
        }
        batteryReceiverRegistered = true
    }

    private fun releaseRuntimeResources(): AppResult<Unit> {
        alarmJobs.forEach(Job::cancel)
        alarmJobs = emptyList()
        val results = mutableListOf<AppResult<*>>()
        results += appResult(AppErrorCode.MEASUREMENT, "Release measurement wake lock") {
            wakeLock?.takeIf(PowerManager.WakeLock::isHeld)?.release()
            wakeLock = null
        }
        results += appResult(AppErrorCode.MEASUREMENT, "Unregister low battery receiver") {
            if (batteryReceiverRegistered) unregisterReceiver(lowBatteryReceiver)
            batteryReceiverRegistered = false
        }
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Release recording host resources")
    }

    override fun onDestroy() {
        val config = activeConfig
        if (config != null && !isFinishing) {
            isFinishing = true
            val cleanup = runBlocking(Dispatchers.IO) {
                controller?.stop(RecordingStopReason.PLATFORM_DESTROYED) ?: AppResult.success(Unit)
            }
            val resources = releaseRuntimeResources()
            sessionStore.markIdle()
            sessionStore.publishStopped(
                config.sessionId,
                MeasurementStopReason.SERVICE_DESTROYED,
                listOf(cleanup, resources).combineAppResults(
                    AppErrorCode.MEASUREMENT,
                    "Destroy recording foreground host",
                ),
            )
        }
        eventJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun RecordingStopReason.toMeasurementReason(): MeasurementStopReason = when (this) {
        RecordingStopReason.USER_REQUEST,
        RecordingStopReason.PAIRED_ABORT,
        -> MeasurementStopReason.USER_REQUEST

        RecordingStopReason.DURATION_EXPIRED -> MeasurementStopReason.DURATION_EXPIRED

        RecordingStopReason.LOW_BATTERY -> MeasurementStopReason.LOW_BATTERY

        RecordingStopReason.SOURCE_FAILURE -> MeasurementStopReason.SOURCE_FAILURE

        RecordingStopReason.PLATFORM_DESTROYED -> MeasurementStopReason.SERVICE_DESTROYED
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
        const val START_AT_EPOCH_MILLIS = "START_AT_EPOCH_MILLIS"
        const val DURATION_MILLIS = "DURATION_MILLIS"
        const val NOTES = "NOTES"
        const val ALARM_OFFSETS_SECONDS = "ALARM_OFFSETS_SECONDS"
        const val ACTIVITY_RECOGNITION = "ACTIVITY_RECOGNITION"
        const val ACTIVITY_RECOGNITION_PERIOD_SECONDS = "ACTIVITY_RECOGNITION_PERIOD_SECONDS"
        const val SIGNIFICANT_MOTION = "SIGNIFICANT_MOTION"
        const val ACTION_ANNOTATE = "com.motionapps.sensorbox.action.ANNOTATE_MEASUREMENT"
        const val ANNOTATION_TIME = "ANNOTATION_TIME"
        const val ANNOTATION_TEXT = "ANNOTATION_TEXT"
        const val ACTION_STOP = "com.motionapps.sensorbox.action.STOP_MEASUREMENT"

        private const val NOTIFICATION_ID = 729
        private const val WAKE_LOCK_TAG = "SensorBox::Measurement"
    }
}
