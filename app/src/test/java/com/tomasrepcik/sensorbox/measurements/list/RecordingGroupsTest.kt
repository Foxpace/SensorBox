package com.tomasrepcik.sensorbox.measurements.list

import com.tomasrepcik.sensorbox.measurements.storage.MeasurementSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingGroupsTest {
    @Test
    fun `Given a shared session When measurements are listed Then phone and watch appear together`() {
        // Given
        val phone = measurement("renamed-phone", "session", "phone")
        val watch = measurement("imported-watch", "session", "watch")

        // When
        val recordings = MeasurementsState(listOf(watch, phone)).recordings

        // Then
        assertEquals(listOf(listOf(phone, watch)), recordings)
    }

    @Test
    fun `Given identical names and times When sessions differ Then measurements stay separate`() {
        // Given
        val phone = measurement("phone", "first", "phone")
        val watch = measurement("watch", "second", "watch")

        // When
        val recordings = MeasurementsState(listOf(phone, watch)).recordings

        // Then
        assertEquals(2, recordings.size)
    }

    @Test
    fun `Given older measurements When session identity is absent Then no relationship is guessed`() {
        // Given
        val phone = measurement("walk", null, null)
        val watch = measurement("WEAR_walk", null, null)

        // When
        val recordings = MeasurementsState(listOf(phone, watch)).recordings

        // Then
        assertEquals(2, recordings.size)
    }

    private fun measurement(id: String, sessionId: String?, device: String?) = MeasurementSummary(
        id = id,
        name = "walk",
        recordedAtMillis = 100L,
        recordedAtText = "Today",
        fileCount = 1,
        sessionId = sessionId,
        device = device,
    )
}
