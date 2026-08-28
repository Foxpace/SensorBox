package com.tomasrepcik.sensorbox.sensorservices.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tomasrepcik.sensorbox.sensorservices.R

object Notify {
    fun createRecordingNotification(context: Context): Notification {
        createChannel(context)
        val stopIntent = Intent(context, RecordingService::class.java)
            .setAction(RecordingService.ACTION_STOP_RECORDING)
        val stopAction = PendingIntent.getService(
            context,
            STOP_REQUEST_CODE,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_content))
            .setSmallIcon(R.drawable.ic_graph)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(createOpenAppAction(context))
            .addAction(R.drawable.ic_stop, context.getString(R.string.text_stop), stopAction)
            .build()
    }

    private fun createOpenAppAction(context: Context): PendingIntent? {
        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            OPEN_APP_REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private const val CHANNEL_ID = "recording"
    private const val OPEN_APP_REQUEST_CODE = 10
    private const val STOP_REQUEST_CODE = 20
}
