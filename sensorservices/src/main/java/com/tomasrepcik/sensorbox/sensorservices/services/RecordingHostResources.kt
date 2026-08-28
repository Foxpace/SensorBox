package com.tomasrepcik.sensorbox.sensorservices.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.os.Build
import android.os.PowerManager
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.recording.RecordingStopReason
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class RecordingHostResources(
    private val service: Service,
    private val scope: CoroutineScope,
    private val requestStop: (RecordingStopReason) -> Unit,
    private val playAlarm: () -> Unit,
) {
    private var wakeLock: PowerManager.WakeLock? = null
    private var batteryReceiverRegistered = false
    private var alarmJobs: List<Job> = emptyList()

    private val lowBatteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_LOW) requestStop(RecordingStopReason.LOW_BATTERY)
        }
    }

    fun start(request: RecordingRequest) {
        promoteToForeground(request)
        if (request.requiresWakeLock) acquireWakeLock()
        if (request.stopOnLowBattery) registerLowBatteryReceiver()
    }

    fun scheduleAlarms(offsetsSeconds: List<Int>) {
        alarmJobs = offsetsSeconds.distinct().sorted().map { seconds ->
            scope.launch {
                delay(seconds * 1_000L)
                playAlarm()
            }
        }
    }

    fun release(): AppResult<Unit> {
        alarmJobs.forEach(Job::cancel)
        alarmJobs = emptyList()
        return listOf(releaseWakeLock(), unregisterLowBatteryReceiver())
            .combineAppResults(AppErrorCode.RECORDING, "Release recording host resources")
    }

    fun removeNotification() {
        service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
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
        FOREGROUND_SERVICE_TYPE_HEALTH or FOREGROUND_SERVICE_TYPE_LOCATION
    } else {
        FOREGROUND_SERVICE_TYPE_HEALTH
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
