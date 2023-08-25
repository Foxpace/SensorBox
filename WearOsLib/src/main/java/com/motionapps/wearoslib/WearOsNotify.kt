package com.motionapps.wearoslib

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.motionapps.wearoslib.WearOsConstants.STOP_SYNC

/**
 * Functions to create and update notifications connected with Wear Os device
 */
object WearOsNotify {

    private const val CHANNEL_ID = "sensorBoxId"
    private const val CHANNEL_NAME = "SensorBoxChannel"

    /**
     * notification for the synchronization between phone and wearable
     * creates channel and notification -
     *
     * @param context
     * @param count - number of files to move
     * @param progress - actual number of files
     * @param importance - importance
     * @return - built notification
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    fun createProgressNotification(
        context: Context,
        count: Int,
        progress: Int,
        importance: Int
    ): Notification {
        createChannel(context)
        val builder = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
        // it is ongoing notification for service
        builder.setAutoCancel(false)
        builder.setOngoing(true)
        builder.setOnlyAlertOnce(true)
        builder.setProgress(count, progress, false)
        builder.setContentTitle(context.getString(R.string.wear_os_sync_data))

        // importance
        builder.priority = importance
        builder.setCategory(Notification.CATEGORY_SERVICE)

        // button to stop sync
        builder.setSmallIcon(R.drawable.ic_graph)
        builder.color = ContextCompat.getColor(context, R.color.black_color)
        val stopIntent = Intent(STOP_SYNC)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.addAction(
                R.drawable.ic_baseline_stop,
                context.getString(R.string.text_stop),
                PendingIntent.getBroadcast(
                    context,
                    25, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }else{
            builder.addAction(
                R.drawable.ic_baseline_stop,
                context.getString(R.string.text_stop),
                PendingIntent.getBroadcast(
                    context,
                    25, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
        return builder.build()
    }

    /**
     * updates notification by id
     *
     * @param context
     * @param id - id of the notification
     * @param notification - built notification
     */
    fun updateNotification(context: Context, id: Int, notification: Notification) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, notification)
    }

    /**
     * cancels notification by id
     *
     * @param context
     * @param id - of the notification
     */
    fun cancelNotification(context: Context, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }

    /**
     * creates notification channel for the Oreo and higher of the Android
     *
     * @param context
     */
    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

}