package com.motionapps.sensorbox.presentation.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainReducerTest {
    @Test
    fun `Given an unselected sensor When toggled Then it becomes selected`() {
        val givenState = MainStateFixtures.state()

        val actual = MainReducer.reduce(givenState, MainIntent.ToggleSensor(sensorId = 1))

        assertTrue(1 in actual.state.selectedSensorIds)
    }

    @Test
    fun `Given record state When storage is requested Then picker effect is emitted`() {
        val givenState = MainStateFixtures.state()

        val actual = MainReducer.reduce(givenState, MainIntent.ChooseStorage)

        assertEquals(MainEffect.PickStorageDirectory, actual.effect)
    }

    @Test
    fun `Given GPS disabled When toggled Then GPS becomes enabled`() {
        val givenState = MainStateFixtures.state(includesGps = false)

        val actual = MainReducer.reduce(givenState, MainIntent.ToggleGps)

        assertTrue(actual.state.includesGps)
    }

    @Test
    fun `Given an unselected Wear sensor When toggled Then only Wear selection changes`() {
        val givenState = MainStateFixtures.state()

        val actual = MainReducer.reduce(givenState, MainIntent.ToggleWearSensor(sensorId = 21))

        assertTrue(21 in actual.state.selectedWearSensorIds)
        assertTrue(actual.state.selectedSensorIds.isEmpty())
    }

    @Test
    fun `Given endless mode When timed mode is selected Then timing configuration is retained`() {
        val givenState = MainStateFixtures.state().copy(durationSeconds = 60)

        val actual = MainReducer.reduce(givenState, MainIntent.SetMeasurementType("TIMED"))

        assertEquals("TIMED", actual.state.measurementType)
        assertEquals(60, actual.state.durationSeconds)
    }

    @Test
    fun `Given selected sensors When setup is opened Then setup route is shown`() {
        val givenState = MainStateFixtures.state(selectedSensorIds = setOf(1))

        val actual = MainReducer.reduce(givenState, MainIntent.OpenMeasurementSetup)

        assertEquals(MainRoute.SETUP, actual.state.route)
    }

    @Test
    fun `Given a sensor When its information is opened Then details route retains its type`() {
        val givenState = MainStateFixtures.state()

        val actual = MainReducer.reduce(givenState, MainIntent.OpenSensorDetails(sensorType = 1))

        assertEquals(MainRoute.SENSOR_DETAILS, actual.state.route)
        assertEquals(1, actual.state.detailsSensorType)
    }

    @Test
    fun `Given measurement setup When returning Then sensor selection is shown`() {
        val givenState = MainStateFixtures.state(route = MainRoute.SETUP, selectedSensorIds = setOf(1))

        val actual = MainReducer.reduce(givenState, MainIntent.ReturnToSensorSelection)

        assertEquals(MainRoute.RECORD, actual.state.route)
    }

    @Test
    fun `Given later introduction page When going back Then previous page is shown`() {
        val givenState = MainStateFixtures.state().copy(onboardingPage = 3)

        val actual = MainReducer.reduce(givenState, MainIntent.RetreatOnboarding)

        assertEquals(2, actual.state.onboardingPage)
    }

    @Test
    fun `Given policy introduction When privacy is opened Then privacy effect is emitted`() {
        val givenState = MainStateFixtures.state()

        val actual = MainReducer.reduce(givenState, MainIntent.OpenPrivacyPolicy)

        assertEquals(MainEffect.OpenPrivacyPolicy, actual.effect)
    }

    @Test
    fun `Given policy introduction When terms are opened Then terms effect is emitted`() {
        val givenState = MainStateFixtures.state()

        val actual = MainReducer.reduce(givenState, MainIntent.OpenTermsOfUse)

        assertEquals(MainEffect.OpenTermsOfUse, actual.effect)
    }

    @Test
    fun `Given battery introduction When exemption is requested Then settings effect is emitted`() {
        val givenState = MainStateFixtures.state()

        val actual = MainReducer.reduce(givenState, MainIntent.RequestBatteryOptimizationExemption)

        assertEquals(MainEffect.RequestBatteryOptimizationExemption, actual.effect)
    }
}
