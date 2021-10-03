package com.motionapps.sensorservices.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.motionapps.sensorservices.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
object Notify {
    private const val CHANNEL_ID = "SensorBoxId"
    private const val CHANNEL_NAME = "SensorBoxChannel"

    /**
     * basic notification for foreground of MeasurementService
     *
     * @param context
     * @param title - title of the notification
     * @param content - subtext of notification
     * @return built notification
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    fun createNotification(context: Context, title: String?, content: String?): Notification {

        createChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setContentTitle(title)
        builder.setAutoCancel(false)
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(content))

        builder.setSmallIcon(R.drawable.ic_graph)
        builder.color = ContextCompat.getColor(context, R.color.colorBlack)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.priority = NotificationManager.IMPORTANCE_DEFAULT
            builder.setCategory(Notification.CATEGORY_SERVICE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val stopIntent = Intent(MeasurementService.STOP_SERVICE)
            val stopPendingIntent = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                PendingIntent.getBroadcast(context, 20, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            }else{
                PendingIntent.getBroadcast(context, 20, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

            builder.addAction(R.drawable.ic_stop, context.getString(R.string.text_stop), stopPendingIntent)
        }

        return builder.build()
    }

    /**
     * notification at the end of the measurement
     *
     * @param context
     * @param title - title of the notification
     * @param content - subtext of notification
     * @return built notification
     */
    fun endingNotification(context: Context, title: String?, content: String?): Notification {

        createChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        builder.setContentTitle(title)
        builder.setAutoCancel(false)
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(content))
        builder.setSound(Uri.parse("android.resource://" + context.packageName + "/" + R.raw.end))

        builder.setSmallIcon(R.drawable.ic_graph)
        builder.color = ContextCompat.getColor(context, R.color.colorBlack)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.priority = NotificationManager.IMPORTANCE_DEFAULT
            builder.setCategory(Notification.CATEGORY_SERVICE)
        }

        return builder.build()
    }


    /**
     * replaces notification
     *
     * @param context
     * @param id - id of the notification
     * @param notification - notification to to be placed
     */
    fun updateNotification(context: Context, id: Int, notification: Notification) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, notification)
    }

    /**
     * cancels notification by id
     *
     * @param context
     * @param id - of notification to cancel
     */
    fun cancelNotification(context: Context, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
    }


    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }
}