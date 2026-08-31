package com.tomasrepcik.sensorbox.recordinghost.sources.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransitionResult

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let(ActivityRecognitionCallbacks::receive)
    }
}

internal class ActivityRecognitionCallbackRegistration(
    val onUpdate: (ActivityUpdate) -> Unit,
    val onTransitions: (List<ActivityTransitionSample>) -> Unit,
)

internal object ActivityRecognitionCallbacks {
    @Volatile
    private var active: ActivityRecognitionCallbackRegistration? = null

    fun register(
        onUpdate: (ActivityUpdate) -> Unit,
        onTransitions: (List<ActivityTransitionSample>) -> Unit,
    ): ActivityRecognitionCallbackRegistration =
        ActivityRecognitionCallbackRegistration(onUpdate, onTransitions).also { registration ->
            active = registration
        }

    fun unregister(registration: ActivityRecognitionCallbackRegistration) {
        synchronized(this) {
            if (active === registration) active = null
        }
    }

    fun receive(intent: Intent) {
        val registration = active ?: return
        when (intent.action) {
            ACTIVITY_UPDATE_ACTION -> receiveUpdate(intent, registration)
            ACTIVITY_TRANSITION_ACTION -> receiveTransitions(intent, registration)
        }
    }

    private fun receiveUpdate(intent: Intent, registration: ActivityRecognitionCallbackRegistration) {
        val result = ActivityRecognitionResult.extractResult(intent) ?: return
        registration.onUpdate(
            ActivityUpdate(
                elapsedRealtimeMillis = result.elapsedRealtimeMillis,
                confidences = ACTIVITY_CONFIDENCE_TYPES.map(result::getActivityConfidence),
            ),
        )
    }

    private fun receiveTransitions(intent: Intent, registration: ActivityRecognitionCallbackRegistration) {
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        registration.onTransitions(
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
