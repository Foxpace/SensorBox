package com.motionapps.sensorbox.presentation.main

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
    fun `Given record state When storage is requested Then picker effect is emitted`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.ChooseStorage)

        assertEquals(RecordingEffect.PickStorageDirectory, actual.effect)
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

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.ToggleWearSensor(sensorId = 21))

        assertTrue(21 in actual.state.selectedWearSensorIds)
        assertTrue(actual.state.selectedSensorIds.isEmpty())
    }

    @Test
    fun `Given endless mode When timed mode is selected Then timing configuration is retained`() {
        val givenState = RecordingStateFixtures.state().copy(durationSeconds = 60)

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.SetMeasurementType("TIMED"))

        assertEquals("TIMED", actual.state.measurementType)
        assertEquals(60, actual.state.durationSeconds)
    }

    @Test
    fun `Given selected sensors When setup is opened Then setup route is shown`() {
        val givenState = RecordingStateFixtures.state(selectedSensorIds = setOf(1))

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.OpenMeasurementSetup)

        assertEquals(RecordingEffect.Navigate(MainRoute.SETUP), actual.effect)
    }

    @Test
    fun `Given a sensor When its information is opened Then details route retains its type`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.OpenSensorDetails(sensorType = 1))

        assertEquals(RecordingEffect.Navigate(MainRoute.SENSOR_DETAILS), actual.effect)
        assertEquals(1, actual.state.detailsSensorType)
    }

    @Test
    fun `Given measurement setup When returning Then sensor selection is shown`() {
        val givenState = RecordingStateFixtures.state(selectedSensorIds = setOf(1))

        val actual = RecordingReducer.reduce(givenState, RecordingIntent.ReturnToSensorSelection)

        assertEquals(RecordingEffect.Navigate(MainRoute.RECORD), actual.effect)
    }
}
