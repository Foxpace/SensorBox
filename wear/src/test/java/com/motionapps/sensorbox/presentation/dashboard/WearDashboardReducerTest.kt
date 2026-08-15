package com.motionapps.sensorbox.presentation.dashboard

import com.motionapps.sensorbox.core.testing.AppPreferencesFixtures
import com.motionapps.sensorbox.presentation.menu.WearMenuDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearDashboardReducerTest {
    @Test
    fun `Given menu When record is opened Then record route is selected`() {
        val givenState = WearDashboardState()

        val actual = WearDashboardReducer.reduce(
            givenState,
            WearDashboardIntent.Open(WearMenuDestination.RECORD),
        )

        assertEquals(WearRoute.RECORD, actual.route)
    }

    @Test
    fun `Given no selected sensor When toggled Then sensor becomes selected`() {
        val givenState = WearDashboardState(
            preferences = AppPreferencesFixtures.preferences(gpsIntervalSeconds = 30),
        )

        val actual = WearDashboardReducer.reduce(givenState, WearDashboardIntent.ToggleSensor(21))

        assertTrue(21 in actual.selectedSensorIds)
        assertEquals(30, actual.preferences.gpsIntervalSeconds)
    }

    @Test
    fun `Given live picker When sensor is chosen Then chart route retains sensor`() {
        val givenState = WearDashboardState(route = WearRoute.LIVE)

        val actual = WearDashboardReducer.reduce(givenState, WearDashboardIntent.ObserveSensor(4))

        assertEquals(WearRoute.LIVE, actual.route)
        assertEquals(4, actual.liveSensorType)
    }
}
