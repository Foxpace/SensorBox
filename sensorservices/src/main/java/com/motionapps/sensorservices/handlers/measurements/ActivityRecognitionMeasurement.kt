package com.motionapps.sensorservices.handlers.measurements

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorservices.handlers.StorageHandler
import java.io.OutputStream

/** Records periodic Google Activity Recognition confidence values. */
class ActivityRecognitionMeasurement(private val periodSeconds: Int) {
    private var client: ActivityRecognitionClient? = null
    private var updatesPendingIntent: PendingIntent? = null
    private var transitionsPendingIntent: PendingIntent? = null
    private var updatesOutput: OutputStream? = null
    private var transitionsOutput: OutputStream? = null
    private var receiverRegistered = false
    private var writeFailure: AppError? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_UPDATE -> writeActivityUpdate(intent)
                ACTION_TRANSITION -> writeActivityTransitions(intent)
            }
        }
    }

    private fun writeActivityUpdate(intent: Intent) {
        val result = ActivityRecognitionResult.extractResult(intent) ?: return
        val confidences = ACTIVITIES.joinToString(";") { result.getActivityConfidence(it).toString() }
        recordWriteFailure("Write activity update") {
            updatesOutput?.write("${result.elapsedRealtimeMillis};$confidences\n".toByteArray())
        }
    }

    private fun writeActivityTransitions(intent: Intent) {
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        recordWriteFailure("Write activity transition") {
            result.transitionEvents.forEach { transition ->
                transitionsOutput?.write(
                    "${transition.elapsedRealTimeNanos};${transition.activityType};${transition.transitionType}\n"
                        .toByteArray(),
                )
            }
        }
    }

    fun prepare(context: Context, folderName: String, useInternalStorage: Boolean): AppResult<Unit> {
        val updatesResult = if (useInternalStorage) {
            StorageHandler.createFileInInternalFolder(context, folderName, UPDATES_FILE_NAME)
        } else {
            StorageHandler.createFileInFolder(context, folderName, "text/csv", UPDATES_FILE_NAME)
        }
        return updatesResult.flatMap { updates ->
            updatesOutput = updates
            val transitionsResult = if (useInternalStorage) {
                StorageHandler.createFileInInternalFolder(context, folderName, TRANSITIONS_FILE_NAME)
            } else {
                StorageHandler.createFileInFolder(context, folderName, "text/csv", TRANSITIONS_FILE_NAME)
            }
            transitionsResult.onFailure {
                appResult(AppErrorCode.STORAGE, "Close incomplete activity measurement") { updates.close() }
            }.flatMap { transitions ->
                transitionsOutput = transitions
                initializeResources(context)
            }
        }.withAppError(AppErrorCode.MEASUREMENT, "Initialize activity recognition")
    }

    private fun initializeResources(context: Context): AppResult<Unit> = appResult(
        AppErrorCode.MEASUREMENT,
        "Initialize activity recognition resources",
    ) {
        updatesOutput?.write(
            "t_elapsed;still;on_foot;walking;running;vehicle;bike;unknown;tilting\n".toByteArray(),
        )
        transitionsOutput?.write("t_nanos;activity;enter_exit\n".toByteArray())
        val filter = IntentFilter(ACTION_UPDATE).apply { addAction(ACTION_TRANSITION) }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        client = ActivityRecognition.getClient(context)
        updatesPendingIntent = PendingIntent.getBroadcast(
            context,
            UPDATES_REQUEST_CODE,
            Intent(ACTION_UPDATE).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        transitionsPendingIntent = PendingIntent.getBroadcast(
            context,
            TRANSITIONS_REQUEST_CODE,
            Intent(ACTION_TRANSITION).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    @SuppressLint("MissingPermission")
    fun start(context: Context): AppResult<Unit> {
        if (!hasPermission(context)) {
            return AppResult.failure(AppError(AppErrorCode.PERMISSION, "Start activity recognition"))
        }
        return appResult(AppErrorCode.MEASUREMENT, "Start activity recognition") {
            updatesPendingIntent?.let {
                client?.requestActivityUpdates(periodSeconds.coerceAtLeast(1) * 1_000L, it)
                    ?.addOnFailureListener { error ->
                        AppError.from(AppErrorCode.MEASUREMENT, "Request activity updates", error)
                    }
            }
            transitionsPendingIntent?.let {
                client?.requestActivityTransitionUpdates(ActivityTransitionRequest(ACTIVITY_TRANSITIONS), it)
                    ?.addOnFailureListener { error ->
                        AppError.from(AppErrorCode.MEASUREMENT, "Request activity transitions", error)
                    }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun pause(context: Context): AppResult<Unit> = appResult(
        AppErrorCode.MEASUREMENT,
        "Pause activity recognition",
    ) {
        if (hasPermission(context)) {
            updatesPendingIntent?.let {
                client?.removeActivityUpdates(it)?.addOnFailureListener { error ->
                    AppError.from(AppErrorCode.MEASUREMENT, "Remove activity updates", error)
                }
            }
            transitionsPendingIntent?.let {
                client?.removeActivityTransitionUpdates(it)?.addOnFailureListener { error ->
                    AppError.from(AppErrorCode.MEASUREMENT, "Remove activity transitions", error)
                }
            }
        }
        if (receiverRegistered) {
            context.unregisterReceiver(receiver)
        }
        receiverRegistered = false
    }

    private suspend fun save(): AppResult<Unit> {
        val results = mutableListOf<AppResult<*>>()
        results += appResult(AppErrorCode.STORAGE, "Close activity updates") {
            updatesOutput?.close()
        }
        results += appResult(AppErrorCode.STORAGE, "Close activity transitions") {
            transitionsOutput?.close()
        }
        writeFailure?.let { results += AppResult.failure(it) }
        updatesOutput = null
        transitionsOutput = null
        writeFailure = null
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Save activity recognition")
    }

    suspend fun stop(context: Context): AppResult<Unit> {
        val results = listOf(pause(context), save())
        updatesPendingIntent = null
        transitionsPendingIntent = null
        client = null
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Stop activity recognition")
    }

    private inline fun recordWriteFailure(operation: String, block: () -> Unit) {
        if (writeFailure != null) return
        appResult(AppErrorCode.STORAGE, operation, block).onFailure { writeFailure = it }
    }

    private fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val ACTION_UPDATE = "com.motionapps.sensorbox.ACTIVITY_RECOGNITION_UPDATE"
        const val ACTION_TRANSITION = "com.motionapps.sensorbox.ACTIVITY_RECOGNITION_TRANSITION"
        const val UPDATES_REQUEST_CODE = 457
        const val TRANSITIONS_REQUEST_CODE = 1_654
        const val UPDATES_FILE_NAME = "activity_updates.csv"
        const val TRANSITIONS_FILE_NAME = "activity_transitions.csv"
        val ACTIVITIES = intArrayOf(
            DetectedActivity.STILL,
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.UNKNOWN,
            DetectedActivity.TILTING,
        )
        val ACTIVITY_TRANSITIONS = ACTIVITIES.flatMap { activity ->
            listOf(
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build(),
                ActivityTransition.Builder()
                    .setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build(),
            )
        }
    }
}
