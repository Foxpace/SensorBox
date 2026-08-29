package com.tomasrepcik.sensorbox.home

import com.tomasrepcik.sensorbox.core.testing.AppPreferencesFixtures
import com.tomasrepcik.sensorbox.menu.WearMenuDestination
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
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
        assertEquals(30, actual.preferences.recording.gpsIntervalSeconds)
    }

    @Test
    fun `Given menu When sync is selected Then transfer screen is opened`() {
        val actual = WearDashboardReducer.reduce(
            WearDashboardState(),
            WearDashboardIntent.Open(WearMenuDestination.SYNC),
        )

        assertEquals(WearRoute.SETTINGS, actual.route)
    }

    @Test
    fun `Given live picker When sensor is chosen Then chart route retains sensor`() {
        val givenState = WearDashboardState(route = WearRoute.LIVE)

        val actual = WearDashboardReducer.reduce(givenState, WearDashboardIntent.ObserveSensor(4))

        assertEquals(WearRoute.LIVE, actual.route)
        assertEquals(4, actual.liveSensorType)
    }

    @Test
    fun `Given recording screen When recording finishes Then menu with sync is restored`() {
        val active = WearDashboardReducer.recordingSessionChanged(
            WearDashboardState(route = WearRoute.RECORD),
            RecordingSessionState.Running("session", "fixture", 1L, listOf(1), false),
        )

        val finished = WearDashboardReducer.recordingSessionChanged(active, RecordingSessionState.Idle)

        assertEquals(WearRoute.ACTIVE, active.route)
        assertEquals(WearRoute.MENU, finished.route)
    }
}
