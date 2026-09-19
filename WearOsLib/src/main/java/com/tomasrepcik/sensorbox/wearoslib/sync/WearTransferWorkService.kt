package com.tomasrepcik.sensorbox.wearoslib.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Keeps transfers alive without occupying the WearableListenerService callback looper. */
class WearTransferWorkService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var running = 0
    private var latestStartId = 0

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Watch sync", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        startForeground(
            NOTIFICATION_ID,
            notification.setContentTitle("Syncing watch measurements")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOngoing(true)
                .build(),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        val work = intent?.getStringExtra(WORK_ID)?.let(pending::remove)
        if (work != null) {
            running += 1
            scope.launch {
                try {
                    work()
                } finally {
                    running -= 1
                    if (running == 0) stopSelfResult(latestStartId)
                }
            }
        } else if (running == 0) {
            stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "watch_measurement_sync"
        private const val NOTIFICATION_ID = 2402
        private const val WORK_ID = "workId"
        private val pending = ConcurrentHashMap<String, suspend () -> Unit>()

        fun enqueue(context: Context, work: suspend () -> Unit): AppResult<Unit> {
            val id = UUID.randomUUID().toString()
            pending[id] = work
            return appResult(AppErrorCode.CONNECTIVITY, "Start watch sync") {
                val intent = Intent(context, WearTransferWorkService::class.java).putExtra(WORK_ID, id)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Unit
            }.onFailure { pending.remove(id) }
        }
    }
}
