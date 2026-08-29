package com.tomasrepcik.sensorbox.recordinghost.session

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Build
import android.os.PowerManager
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.failure.combineAppResults
import com.tomasrepcik.sensorbox.recording.RecordingStopReason
import com.tomasrepcik.sensorbox.recordinghost.notification.Notify
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class RecordingHostResources(
    private val service: Service,
    private val scope: CoroutineScope,
    private val requestStop: (RecordingStopReason) -> Unit,
    private val playAlarm: () -> Unit,
) : RecordingHostEnvironment {
    private var wakeLock: PowerManager.WakeLock? = null
    private var batteryReceiverRegistered = false
    private var alarmJobs: List<Job> = emptyList()

    private val lowBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_LOW) requestStop(RecordingStopReason.LOW_BATTERY)
        }
    }

    override fun start(request: RecordingRequest) {
        promoteToForeground(request)
        if (request.requiresWakeLock) acquireWakeLock()
        if (request.stopOnLowBattery) registerLowBatteryReceiver()
    }

    override fun scheduleAlarms(offsetsSeconds: List<Int>) {
        alarmJobs = offsetsSeconds.distinct().sorted().map { seconds ->
            scope.launch {
                delay(seconds * 1_000L)
                playAlarm()
            }
        }
    }

    override fun release(): AppResult<Unit> {
        alarmJobs.forEach(Job::cancel)
        alarmJobs = emptyList()
        return listOf(releaseWakeLock(), unregisterLowBatteryReceiver())
            .combineAppResults(AppErrorCode.RECORDING, "Release recording host resources")
    }

    override fun removeNotification() {
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    override fun stopService() {
        service.stopSelf()
    }

    private fun promoteToForeground(request: RecordingRequest) {
        val notification = Notify.createRecordingNotification(service)
        if (Build.VERSION.SDK_INT >= 34) {
            service.startForeground(NOTIFICATION_ID, notification, foregroundTypes(request))
        } else {
            service.startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun foregroundTypes(request: RecordingRequest): Int = if (request.includesGps) {
        FOREGROUND_SERVICE_TYPE_SPECIAL_USE or FOREGROUND_SERVICE_TYPE_LOCATION
    } else {
        FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    }

    private fun acquireWakeLock() {
        val powerManager = service.getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply { acquire() }
    }

    private fun registerLowBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_LOW)
        if (Build.VERSION.SDK_INT >= 33) {
            service.registerReceiver(lowBatteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            service.registerReceiver(lowBatteryReceiver, filter)
        }
        batteryReceiverRegistered = true
    }

    private fun releaseWakeLock(): AppResult<Unit> = appResult(
        AppErrorCode.RECORDING,
        "Release recording wake lock",
    ) {
        wakeLock?.takeIf(PowerManager.WakeLock::isHeld)?.release()
        wakeLock = null
    }

    private fun unregisterLowBatteryReceiver(): AppResult<Unit> = appResult(
        AppErrorCode.RECORDING,
        "Unregister low battery receiver",
    ) {
        if (batteryReceiverRegistered) service.unregisterReceiver(lowBatteryReceiver)
        batteryReceiverRegistered = false
    }

    private companion object {
        const val NOTIFICATION_ID = 729
        const val WAKE_LOCK_TAG = "SensorBox::Recording"
    }
}
