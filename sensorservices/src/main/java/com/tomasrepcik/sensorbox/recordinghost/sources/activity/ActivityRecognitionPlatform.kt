package com.tomasrepcik.sensorbox.recordinghost.sources.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
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
    private var callbacks: ActivityRecognitionCallbackRegistration? = null

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
                ActivityTransitionRequest(activityTransitions),
                transitions,
            ).await()
        }
    }

    private fun prepareResources(
        onUpdate: (ActivityUpdate) -> Unit,
        onTransitions: (List<ActivityTransitionSample>) -> Unit,
    ): AppResult<Unit> = appResult(AppErrorCode.RECORDING, "Initialize activity recognition resources") {
        client = ActivityRecognition.getClient(context)
        updatesPendingIntent = createActivityRecognitionPendingIntent(
            context,
            UPDATES_REQUEST_CODE,
            ACTIVITY_UPDATE_ACTION,
        )
        transitionsPendingIntent = createActivityRecognitionPendingIntent(
            context,
            TRANSITIONS_REQUEST_CODE,
            ACTIVITY_TRANSITION_ACTION,
        )
        callbacks = ActivityRecognitionCallbacks.register(onUpdate, onTransitions)
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
            callbacks?.let(ActivityRecognitionCallbacks::unregister)
            client = null
            updatesPendingIntent = null
            transitionsPendingIntent = null
            callbacks = null
        }
        return results.combineAppResults(AppErrorCode.RECORDING, "Pause activity recognition")
    }

    private fun hasPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 29) {
            Manifest.permission.ACTIVITY_RECOGNITION
        } else {
            LEGACY_ACTIVITY_RECOGNITION_PERMISSION
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val UPDATES_REQUEST_CODE = 457
        const val TRANSITIONS_REQUEST_CODE = 1_654
    }
}

internal const val ACTIVITY_UPDATE_ACTION = "com.tomasrepcik.sensorbox.ACTIVITY_RECOGNITION_UPDATE"
internal const val ACTIVITY_TRANSITION_ACTION = "com.tomasrepcik.sensorbox.ACTIVITY_RECOGNITION_TRANSITION"
internal const val LEGACY_ACTIVITY_RECOGNITION_PERMISSION =
    "com.google.android.gms.permission.ACTIVITY_RECOGNITION"

internal val ACTIVITY_CONFIDENCE_TYPES = intArrayOf(
    DetectedActivity.STILL,
    DetectedActivity.ON_FOOT,
    DetectedActivity.WALKING,
    DetectedActivity.RUNNING,
    DetectedActivity.IN_VEHICLE,
    DetectedActivity.ON_BICYCLE,
    DetectedActivity.UNKNOWN,
    DetectedActivity.TILTING,
)

internal val ACTIVITY_TRANSITION_TYPES = intArrayOf(
    DetectedActivity.IN_VEHICLE,
    DetectedActivity.ON_FOOT,
    DetectedActivity.RUNNING,
    DetectedActivity.WALKING,
    DetectedActivity.ON_BICYCLE,
    DetectedActivity.STILL,
)

private val activityTransitions = ACTIVITY_TRANSITION_TYPES.flatMap { activity ->
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

internal fun createActivityRecognitionPendingIntent(
    context: Context,
    requestCode: Int,
    action: String,
): PendingIntent = PendingIntent.getBroadcast(
    context,
    requestCode,
    Intent(context, ActivityRecognitionReceiver::class.java).setAction(action),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
)
