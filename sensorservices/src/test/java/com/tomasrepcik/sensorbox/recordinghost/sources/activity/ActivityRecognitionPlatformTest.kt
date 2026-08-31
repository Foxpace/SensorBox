package com.tomasrepcik.sensorbox.recordinghost.sources.activity

import com.google.android.gms.location.DetectedActivity
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ActivityRecognitionPlatformTest {
    @Test
    fun `Given activity updates When confidence types are requested Then every CSV activity is included`() {
        // Given
        val expected = intArrayOf(
            DetectedActivity.STILL,
            DetectedActivity.ON_FOOT,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.UNKNOWN,
            DetectedActivity.TILTING,
        )

        // When
        val actual = ACTIVITY_CONFIDENCE_TYPES

        // Then
        assertArrayEquals(expected, actual)
    }

    @Test
    fun `Given transition updates When activity types are requested Then only supported types are included`() {
        // Given
        val expected = intArrayOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_FOOT,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.STILL,
        )

        // When
        val actual = ACTIVITY_TRANSITION_TYPES

        // Then
        assertArrayEquals(expected, actual)
    }
}
