package com.tomasrepcik.sensorbox.home

import com.tomasrepcik.sensorbox.core.testing.AppPreferencesFixtures
import com.tomasrepcik.sensorbox.menu.WearMenuDestination
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearDashboardReducerTest {
    @Test
    fun `Given a previously viewed sensor When live preview is reopened Then the picker has no old selection`() {
        // Given
        val previous = WearDashboardState(route = WearRoute.LIVE, liveSensorType = 1, liveSamples = listOf(listOf(2f)))
        val menu = WearDashboardReducer.reduce(previous, WearDashboardIntent.Back)

        // When
        val reopened = WearDashboardReducer.reduce(menu, WearDashboardIntent.Open(WearMenuDestination.LIVE_SENSOR))

        // Then
        assertEquals(WearRoute.LIVE, reopened.route)
        assertEquals(null, reopened.liveSensorType)
        assertTrue(reopened.liveSamples.isEmpty())
    }

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

    @Test
    fun `Given local selections When phone recording starts Then its session is displayed`() {
        // Given
        val state = WearDashboardState(selectedSensorIds = setOf(1, 2, 3), includesGps = true)
        val session = RecordingSessionState.Running("phone", "walk", 1L, listOf(4), false, 60_000L, 3)

        // When
        val active = WearDashboardReducer.recordingSessionChanged(state, session)

        // Then
        assertEquals(session, active.activeSession)
        assertEquals(setOf(1, 2, 3), active.selectedSensorIds)
        assertTrue(active.includesGps)
    }

    @Test
    fun `Given a running session When stopping Then its config remains visible`() {
        // Given
        val session = RecordingSessionState.Running("phone", "walk", 1L, listOf(4), false)
        val active = WearDashboardReducer.recordingSessionChanged(WearDashboardState(), session)

        // When
        val stopping = WearDashboardReducer.recordingSessionChanged(active, RecordingSessionState.Stopping)

        // Then
        assertEquals(session, stopping.activeSession)
        assertTrue(stopping.isStopping)
    }
}
