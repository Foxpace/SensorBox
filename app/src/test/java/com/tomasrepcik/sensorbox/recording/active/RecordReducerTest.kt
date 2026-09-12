package com.tomasrepcik.sensorbox.recording.active

import com.tomasrepcik.sensorbox.recording.RecordingDevice
import com.tomasrepcik.sensorbox.recording.RecordingStateFixtures
import com.tomasrepcik.sensorbox.recording.toDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordReducerTest {
    @Test
    fun `Given phone and connected watch When selecting all Then every sensor is selected`() {
        // Given
        val state = RecordingStateFixtures.state().copy(
            isWatchConnected = true,
            watchSensors = RecordingStateFixtures.state().sensors.map { it.copy(type = 4) },
        )

        // When
        val selected = RecordReducer.reduce(state, RecordIntent.ToggleAllSensors).state

        // Then
        assertEquals(setOf(1), selected.selectedSensorIds)
        assertEquals(setOf(4), selected.selectedWatchSensorIds)
        assertTrue(selected.includesGps)
        assertTrue(selected.watchIncludesGps)
        assertTrue(selected.areAllSensorsSelected)
    }

    @Test
    fun `Given every phone and connected watch sensor selected When toggling all Then every sensor is deselected`() {
        // Given
        val selected = RecordingStateFixtures.state(includesGps = true).copy(
            selectedSensorIds = setOf(1),
            isWatchConnected = true,
            watchSensors = RecordingStateFixtures.state().sensors.map { it.copy(type = 4) },
            selectedWatchSensorIds = setOf(4),
            watchIncludesGps = true,
        )

        // When
        val deselected = RecordReducer.reduce(selected, RecordIntent.ToggleAllSensors).state

        // Then
        assertTrue(deselected.selectedSensorIds.isEmpty())
        assertTrue(deselected.selectedWatchSensorIds.isEmpty())
        assertFalse(deselected.includesGps)
        assertFalse(deselected.watchIncludesGps)
        assertFalse(deselected.areAllSensorsSelected)
    }

    @Test
    fun `Given disconnected watch When selecting all Then watch selection is preserved`() {
        // Given
        val state = RecordingStateFixtures.state().copy(selectedWatchSensorIds = setOf(4))

        // When
        val selected = RecordReducer.reduce(state, RecordIntent.ToggleAllSensors).state

        // Then
        assertEquals(setOf(1), selected.selectedSensorIds)
        assertEquals(setOf(4), selected.selectedWatchSensorIds)
    }

    @Test
    fun `Given an unselected sensor When toggled Then it becomes selected`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordReducer.reduce(givenState, RecordIntent.ToggleSensor(sensorId = 1))

        assertTrue(1 in actual.state.selectedSensorIds)
    }

    @Test
    fun `Given GPS disabled When toggled Then GPS becomes enabled`() {
        val givenState = RecordingStateFixtures.state(includesGps = false)

        val actual = RecordReducer.reduce(givenState, RecordIntent.ToggleGps)

        assertTrue(actual.state.includesGps)
    }

    @Test
    fun `Given an unselected watch sensor When toggled Then only watch selection changes`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordReducer.reduce(givenState, RecordIntent.ToggleWatchSensor(sensorId = 21))

        assertTrue(21 in actual.state.selectedWatchSensorIds)
        assertTrue(actual.state.selectedSensorIds.isEmpty())
    }

    @Test
    fun `Given selected sensors When continuing Then their draft is opened`() {
        val givenState = RecordingStateFixtures.state(selectedSensorIds = setOf(1))

        val actual = RecordReducer.reduce(givenState, RecordIntent.OpenRecordingSetup)

        assertEquals(RecordEffect.OpenRecordingSetup(givenState.toDraft()), actual.effect)
    }

    @Test
    fun `Given a phone sensor When its information is opened Then its source is emitted`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordReducer.reduce(givenState, RecordIntent.OpenSensorDetails(sensorType = 1))

        assertEquals(
            RecordEffect.OpenSensorDetails(1, RecordingDevice.PHONE),
            actual.effect,
        )
    }

    @Test
    fun `Given a watch sensor When its information is opened Then its source is emitted`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordReducer.reduce(
            givenState,
            RecordIntent.OpenSensorDetails(21, RecordingDevice.WATCH),
        )

        assertEquals(
            RecordEffect.OpenSensorDetails(21, RecordingDevice.WATCH),
            actual.effect,
        )
    }
}
