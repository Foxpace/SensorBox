package com.tomasrepcik.sensorbox.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingReducerTest {
    @Test
    fun `Given an unselected sensor When toggled Then it becomes selected`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.ToggleSensor(sensorId = 1))

        assertTrue(1 in actual.state.selectedSensorIds)
    }

    @Test
    fun `Given record state When recording archive is requested Then picker effect is emitted`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.ChooseRecordingArchive)

        assertEquals(RecordingEffect.PickRecordingArchive, actual.effect)
    }

    @Test
    fun `Given GPS disabled When toggled Then GPS becomes enabled`() {
        val givenState = RecordingStateFixtures.state(includesGps = false)

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.ToggleGps)

        assertTrue(actual.state.includesGps)
    }

    @Test
    fun `Given an unselected Wear sensor When toggled Then only Wear selection changes`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.ToggleWatchSensor(sensorId = 21))

        assertTrue(21 in actual.state.selectedWatchSensorIds)
        assertTrue(actual.state.selectedSensorIds.isEmpty())
    }

    @Test
    fun `Given a negative start delay When changed Then it is clamped to zero`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.SetStartDelay(-1))

        assertEquals(0, actual.state.startDelaySeconds)
    }

    @Test
    fun `Given a negative duration When changed Then it is clamped to zero`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.SetDuration(-1))

        assertEquals(0, actual.state.durationSeconds)
    }

    @Test
    fun `Given selected sensors When setup is opened Then setup route is shown`() {
        val givenState = RecordingStateFixtures.state(selectedSensorIds = setOf(1))

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.OpenRecordingSetup)

        assertEquals(RecordingEffect.Navigate(MainRoute.RECORDING_SETUP), actual.effect)
    }

    @Test
    fun `Given a sensor When its information is opened Then details route retains its type`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.OpenSensorDetails(sensorType = 1))

        assertEquals(RecordingEffect.Navigate(MainRoute.SENSOR_DETAILS), actual.effect)
        assertEquals(1, actual.state.detailsSensorType)
    }

    @Test
    fun `Given a Wear sensor When its information is opened Then details retain the Wear source`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingReducer.reduce(
            givenState,
            RecordingIntent.OpenSensorDetails(21, RecordingDevice.WATCH),
        )

        assertEquals(RecordingEffect.Navigate(MainRoute.SENSOR_DETAILS), actual.effect)
        assertEquals(21, actual.state.detailsSensorType)
        assertEquals(RecordingDevice.WATCH, actual.state.detailsDevice)
    }

    @Test
    fun `Given recording setup When returning Then sensor selection is shown`() {
        val givenState = RecordingStateFixtures.state(selectedSensorIds = setOf(1))

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.ReturnToSensorSelection)

        assertEquals(RecordingEffect.Navigate(MainRoute.RECORD), actual.effect)
    }
}
