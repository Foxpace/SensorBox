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
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.suspendFlatMap
import com.motionapps.sensorservices.serviceController.MeasurementConfig
import com.motionapps.sensorservices.serviceController.ServiceController
import com.motionapps.sensorservices.session.MeasurementSessionState
import com.motionapps.sensorservices.session.MeasurementSessionStore
import com.motionapps.wearoslib.WearOsConstants.WEAR_APP_CAPABILITY
import com.motionapps.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MeasurementService : Service() {
    @Inject
    lateinit var sessionStore: MeasurementSessionStore

    @Inject
    lateinit var sendWearMessage: SendWearMessageUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controller = ServiceController()
    private var activeConfig: MeasurementConfig? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isStopping = false
    private var batteryReceiverRegistered = false
    private var startJob: Job? = null
    private var durationJob: Job? = null
    private var alarmJobs: List<Job> = emptyList()

    private val lowBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_LOW) stopMeasurement()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopMeasurement()

            ACTION_ANNOTATE -> controller.annotate(
                intent.getLongExtra(ANNOTATION_TIME, System.currentTimeMillis()),
                intent.getStringExtra(ANNOTATION_TEXT).orEmpty(),
            )

            else -> intent?.takeIf { activeConfig == null }?.let { startIntent ->
                startMeasurement(startIntent).onFailure { stopMeasurement() }
            }
        }

        return START_NOT_STICKY
    }

    private fun startMeasurement(intent: Intent): Result<Unit> = appResult(
        AppError.Kind.MEASUREMENT,
        "Start measurement service",
    ) {
        val config = MeasurementConfig.from(intent)
        activeConfig = config
        promoteToForeground(config)
        publishRunningSession(config)
        configureRuntimeResources(config)
        scheduleMeasurementStart(config)
    }

    private fun scheduleMeasurementStart(config: MeasurementConfig) {
        startJob = serviceScope.launch {
            delay((config.startAtEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            if (controller.start(this@MeasurementService, config).isFailure) {
                stopMeasurement()
                return@launch
            }
            scheduleAlarms(config)
            if (config.durationMillis > 0L) {
                durationJob = serviceScope.launch {
                    delay(config.durationMillis)
                    stopMeasurement()
                }
            }
        }
    }

    private fun scheduleAlarms(config: MeasurementConfig) {
        alarmJobs = config.alarmOffsetsSeconds.distinct().sorted().map { seconds ->
            serviceScope.launch {
                delay(seconds * 1_000L)
                controller.playAlarm()
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

    private fun publishRunningSession(config: MeasurementConfig) {
        val delayMillis = (config.startAtEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        sessionStore.markRunning(
            MeasurementSessionState.Running(
                folderName = config.folderName,
                startedAtElapsedRealtime = SystemClock.elapsedRealtime() + delayMillis,
                sensorIds = config.sensorIds.toList(),
                includesGps = config.includesGps,
            ),
        )
    }

    private fun configureRuntimeResources(config: MeasurementConfig) {
        if (config.useWakeLock) acquireWakeLock()
        if (config.stopOnLowBattery) registerLowBatteryReceiver()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            acquire()
        }
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

    private fun stopMeasurement() {
        if (isStopping) return
        isStopping = true
        sessionStore.markStopping()
        serviceScope.launch {
            val failures = mutableListOf<Throwable>()
            controller.stop(this@MeasurementService).exceptionOrNull()?.let(failures::add)
            stopRemoteMeasurement().exceptionOrNull()?.let(failures::add)
            finishService().exceptionOrNull()?.let(failures::add)
            failures.firstOrNull()?.let { first ->
                failures.drop(1).forEach(first::addSuppressed)
                AppError.from(AppError.Kind.MEASUREMENT, "Stop measurement service", first)
            }
        }
    }

    private suspend fun stopRemoteMeasurement(): Result<Unit> {
        val config = activeConfig ?: return Result.success(Unit)
        if (config.useInternalStorage || !config.controlsWearMeasurement) return Result.success(Unit)
        return WearCommandCodec.encode(WearCommand.StopMeasurement).suspendFlatMap { payload ->
            sendWearMessage(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, payload)
        }
    }

    private fun finishService(): Result<Unit> {
        val results = mutableListOf<Result<*>>()
        results += releaseRuntimeResources()
        activeConfig = null
        isStopping = false
        results += appResult(AppError.Kind.MEASUREMENT, "Publish idle measurement state") {
            sessionStore.markIdle()
        }
        results += appResult(AppError.Kind.MEASUREMENT, "Remove measurement notification") {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        results += appResult(AppError.Kind.MEASUREMENT, "Stop measurement service instance") {
            stopSelf()
        }
        return results.combineAppResults(AppError.Kind.MEASUREMENT, "Finish measurement service")
    }

    private fun releaseRuntimeResources(): Result<Unit> {
        val results = mutableListOf<Result<*>>()
        startJob?.cancel()
        startJob = null
        durationJob?.cancel()
        durationJob = null
        alarmJobs.forEach(Job::cancel)
        alarmJobs = emptyList()
        results += appResult(AppError.Kind.MEASUREMENT, "Release measurement wake lock") {
            wakeLock?.takeIf(PowerManager.WakeLock::isHeld)?.release()
        }
        wakeLock = null
        results += appResult(AppError.Kind.MEASUREMENT, "Unregister low battery receiver") {
            if (batteryReceiverRegistered) unregisterReceiver(lowBatteryReceiver)
        }
        batteryReceiverRegistered = false
        return results.combineAppResults(AppError.Kind.MEASUREMENT, "Release measurement resources")
    }

    override fun onDestroy() {
        if (activeConfig != null && !isStopping) {
            AppError(AppError.Kind.MEASUREMENT, "Measurement service destroyed before cleanup")
        }
        releaseRuntimeResources()
        appResult(AppError.Kind.MEASUREMENT, "Publish destroyed measurement state") { sessionStore.markIdle() }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val FOLDER_NAME = "FOLDER_NAME"
        const val INTERNAL_STORAGE = "INTERNAL_STORAGE"
        const val ANDROID_SENSORS = "ANDROID_SENSORS"
        const val ANDROID_SENSORS_SPEED = "ANDROID_SENSORS_SPEED"
        const val GPS = "GPS_MEASURE"
        const val STOP_ON_LOW_BATTERY = "STOP_ON_LOW_BATTERY"
        const val USE_WAKE_LOCK = "USE_WAKE_LOCK"
        const val GPS_INTERVAL_SECONDS = "GPS_INTERVAL_SECONDS"
        const val GPS_DISTANCE_METERS = "GPS_DISTANCE_METERS"
        const val MEASUREMENT_TYPE = "MEASUREMENT_TYPE"
        const val START_AT_EPOCH_MILLIS = "START_AT_EPOCH_MILLIS"
        const val DURATION_MILLIS = "DURATION_MILLIS"
        const val NOTES = "NOTES"
        const val ALARM_OFFSETS_SECONDS = "ALARM_OFFSETS_SECONDS"
        const val ACTIVITY_RECOGNITION = "ACTIVITY_RECOGNITION"
        const val ACTIVITY_RECOGNITION_PERIOD_SECONDS = "ACTIVITY_RECOGNITION_PERIOD_SECONDS"
        const val SIGNIFICANT_MOTION = "SIGNIFICANT_MOTION"
        const val CONTROLS_WEAR_MEASUREMENT = "CONTROLS_WEAR_MEASUREMENT"
        const val ACTION_ANNOTATE = "com.motionapps.sensorbox.action.ANNOTATE_MEASUREMENT"
        const val ANNOTATION_TIME = "ANNOTATION_TIME"
        const val ANNOTATION_TEXT = "ANNOTATION_TEXT"
        const val ACTION_STOP = "com.motionapps.sensorbox.action.STOP_MEASUREMENT"

        private const val NOTIFICATION_ID = 729
        private const val WAKE_LOCK_TAG = "SensorBox::Measurement"
    }
}
