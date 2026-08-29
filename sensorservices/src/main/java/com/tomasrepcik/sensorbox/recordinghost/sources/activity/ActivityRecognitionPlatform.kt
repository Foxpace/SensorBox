package com.tomasrepcik.sensorbox.recordinghost.sources.activity

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
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.failure.combineAppResults
import com.tomasrepcik.sensorbox.core.failure.suspendAppResult
import kotlinx.coroutines.tasks.await

internal data class ActivityUpdate(val elapsedRealtimeMillis: Long, val confidences: List<Int>)

internal data class ActivityTransitionSample(
    val elapsedRealtimeNanos: Long,
    val activityType: Int,
    val transitionType: Int,
)

internal interface ActivityRecognitionPlatform {
    suspend fun start(
        periodSeconds: Int,
        onUpdate: (ActivityUpdate) -> Unit,
        onTransitions: (List<ActivityTransitionSample>) -> Unit,
    ): AppResult<Unit>

    suspend fun stop(): AppResult<Unit>
}

internal class AndroidActivityRecognitionPlatform(private val context: Context) : ActivityRecognitionPlatform {
    private var client: ActivityRecognitionClient? = null
    private var updatesPendingIntent: PendingIntent? = null
    private var transitionsPendingIntent: PendingIntent? = null
    private var receiverRegistered = false
    private var updateCallback: ((ActivityUpdate) -> Unit)? = null
    private var transitionCallback: ((List<ActivityTransitionSample>) -> Unit)? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_UPDATE -> ActivityRecognitionResult.extractResult(intent)?.let { result ->
                    updateCallback?.invoke(
                        ActivityUpdate(
                            elapsedRealtimeMillis = result.elapsedRealtimeMillis,
                            confidences = ACTIVITIES.map(result::getActivityConfidence),
                        ),
                    )
                }

                ACTION_TRANSITION -> ActivityTransitionResult.extractResult(intent)?.let { result ->
                    transitionCallback?.invoke(
                        result.transitionEvents.map { transition ->
                            ActivityTransitionSample(
                                elapsedRealtimeNanos = transition.elapsedRealTimeNanos,
                                activityType = transition.activityType,
                                transitionType = transition.transitionType,
                            )
                        },
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun start(
        periodSeconds: Int,
        onUpdate: (ActivityUpdate) -> Unit,
        onTransitions: (List<ActivityTransitionSample>) -> Unit,
    ): AppResult<Unit> {
        if (!hasPermission()) {
            return AppResult.failure(AppError(AppErrorCode.PERMISSION, "Start activity recognition"))
        }
        val prepared = prepareResources(onUpdate, onTransitions)
        if (prepared.isFailure) return prepared
        return suspendAppResult(AppErrorCode.RECORDING, "Start activity recognition") {
            val activeClient = checkNotNull(client) { "Activity recognition client is missing" }
            val updates = checkNotNull(updatesPendingIntent) { "Activity update request is missing" }
            val transitions = checkNotNull(transitionsPendingIntent) { "Activity transition request is missing" }
            activeClient.requestActivityUpdates(periodSeconds.coerceAtLeast(1) * 1_000L, updates).await()
            activeClient.requestActivityTransitionUpdates(
                ActivityTransitionRequest(ACTIVITY_TRANSITIONS),
                transitions,
            ).await()
        }
    }

    private fun prepareResources(
        onUpdate: (ActivityUpdate) -> Unit,
        onTransitions: (List<ActivityTransitionSample>) -> Unit,
    ): AppResult<Unit> = appResult(AppErrorCode.RECORDING, "Initialize activity recognition resources") {
        updateCallback = onUpdate
        transitionCallback = onTransitions
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
    override suspend fun stop(): AppResult<Unit> {
        val results = mutableListOf<AppResult<*>>()
        updatesPendingIntent?.let { pendingIntent ->
            results += suspendAppResult(AppErrorCode.RECORDING, "Remove activity updates") {
                client?.removeActivityUpdates(pendingIntent)?.await()
            }
        }
        transitionsPendingIntent?.let { pendingIntent ->
            results += suspendAppResult(AppErrorCode.RECORDING, "Remove activity transitions") {
                client?.removeActivityTransitionUpdates(pendingIntent)?.await()
            }
        }
        results += appResult(AppErrorCode.RECORDING, "Release activity recognition resources") {
            if (receiverRegistered) context.unregisterReceiver(receiver)
            receiverRegistered = false
            client = null
            updatesPendingIntent = null
            transitionsPendingIntent = null
            updateCallback = null
            transitionCallback = null
        }
        return results.combineAppResults(AppErrorCode.RECORDING, "Pause activity recognition")
    }

    private fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
        PackageManager.PERMISSION_GRANTED

    private companion object {
        const val ACTION_UPDATE = "com.tomasrepcik.sensorbox.ACTIVITY_RECOGNITION_UPDATE"
        const val ACTION_TRANSITION = "com.tomasrepcik.sensorbox.ACTIVITY_RECOGNITION_TRANSITION"
        const val UPDATES_REQUEST_CODE = 457
        const val TRANSITIONS_REQUEST_CODE = 1_654
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
