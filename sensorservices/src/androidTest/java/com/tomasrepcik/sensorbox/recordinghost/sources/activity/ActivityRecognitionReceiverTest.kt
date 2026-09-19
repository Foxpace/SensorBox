package com.tomasrepcik.sensorbox.recordinghost.sources.activity

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ActivityRecognitionReceiverTest {
    private var registration: ActivityRecognitionCallbackRegistration? = null

    @After
    fun unregisterCallbacks() {
        registration?.let(ActivityRecognitionCallbacks::unregister)
        registration = null
    }

    @Test
    fun givenGoogleActivityResultWhenPendingIntentIsSentThenConfidenceCallbackReceivesIt() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val callbackReceived = CountDownLatch(1)
        var update: ActivityUpdate? = null
        registration = ActivityRecognitionCallbacks.register(
            onUpdate = { received ->
                update = received
                callbackReceived.countDown()
            },
            onTransitions = {},
        )
        val pendingIntent = createActivityRecognitionPendingIntent(
            context = context,
            requestCode = UPDATE_TEST_REQUEST_CODE,
            action = ACTIVITY_UPDATE_ACTION,
        )
        val result = ActivityRecognitionResult(
            listOf(DetectedActivity(DetectedActivity.WALKING, 83)),
            1L,
            123L,
        )
        val resultIntent = Intent().putExtra(ACTIVITY_RESULT_EXTRA, result)

        // When
        pendingIntent.send(context, 0, resultIntent)

        // Then
        assertTrue(callbackReceived.await(2, TimeUnit.SECONDS))
        assertEquals(
            ActivityUpdate(123L, listOf(0, 0, 83, 0, 0, 0, 0, 0)),
            update,
        )
        pendingIntent.cancel()
    }

    @Test
    fun givenGoogleTransitionResultWhenReceiverHandlesItThenTransitionCallbackReceivesIt() {
        // Given
        var transitions: List<ActivityTransitionSample>? = null
        registration = ActivityRecognitionCallbacks.register(
            onUpdate = {},
            onTransitions = { received -> transitions = received },
        )
        val result = ActivityTransitionResult(
            listOf(
                ActivityTransitionEvent(
                    DetectedActivity.WALKING,
                    ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                    456L,
                ),
            ),
        )
        val resultIntent = Intent(ACTIVITY_TRANSITION_ACTION)
        SafeParcelableSerializer.serializeToIntentExtra(
            result,
            resultIntent,
            ACTIVITY_TRANSITION_RESULT_EXTRA,
        )

        // When
        ActivityRecognitionReceiver().onReceive(null, resultIntent)

        // Then
        assertEquals(
            listOf(
                ActivityTransitionSample(
                    elapsedRealtimeNanos = 456L,
                    activityType = DetectedActivity.WALKING,
                    transitionType = ActivityTransition.ACTIVITY_TRANSITION_ENTER,
                ),
            ),
            transitions,
        )
    }

    private companion object {
        const val UPDATE_TEST_REQUEST_CODE = 4_571
        const val ACTIVITY_RESULT_EXTRA =
            "com.google.android.location.internal.EXTRA_ACTIVITY_RESULT"
        const val ACTIVITY_TRANSITION_RESULT_EXTRA =
            "com.google.android.location.internal.EXTRA_ACTIVITY_TRANSITION_RESULT"
    }
}
