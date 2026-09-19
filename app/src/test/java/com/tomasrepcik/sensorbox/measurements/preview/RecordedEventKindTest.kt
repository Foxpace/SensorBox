package com.tomasrepcik.sensorbox.measurements.preview

import com.google.android.gms.location.DetectedActivity
import com.tomasrepcik.sensorbox.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordedEventKindTest {
    @Test
    fun `Given event columns When preview is selected Then each event uses its detailed list`() {
        // Given
        val columns = listOf(
            listOf("step"),
            listOf("event"),
            listOf("enter_exit", "activity"),
        )

        // When
        val kinds = columns.map(::recordedEventKind)

        // Then
        assertEquals(RecordedEventKind.entries.toSet(), kinds.toSet())
    }

    @Test
    fun `Given continuous sensor columns When preview is selected Then the chart remains available`() {
        // Given
        val columns = listOf("x", "y", "z")

        // When
        val kind = recordedEventKind(columns)

        // Then
        assertNull(kind)
        assertNull(recordedEventKind(listOf("walking", "still", "running")))
    }

    @Test
    fun `Given recorded activity codes When named Then walking and unknown values are readable`() {
        // Given
        val walking = DetectedActivity.WALKING.toDouble()

        // When
        val walkingName = activityNameResource(walking)
        val unknownName = activityNameResource(999.0)

        // Then
        assertEquals(R.string.preview_activity_walking, walkingName)
        assertEquals(R.string.preview_activity_unknown, unknownName)
    }
}
