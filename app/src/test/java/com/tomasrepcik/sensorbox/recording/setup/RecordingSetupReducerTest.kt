package com.tomasrepcik.sensorbox.recording.setup

import com.tomasrepcik.sensorbox.recording.RecordingDraft
import com.tomasrepcik.sensorbox.recording.RecordingStateFixtures
import com.tomasrepcik.sensorbox.recording.toDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingSetupReducerTest {
    @Test
    fun `Given a negative start delay When changed Then it is clamped to zero`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingSetupReducer.reduce(
            givenState,
            RecordingSetupIntent.SetStartDelay(-1),
        )

        assertEquals(0, actual.startDelaySeconds)
    }

    @Test
    fun `Given a negative duration When changed Then it is clamped to zero`() {
        val givenState = RecordingStateFixtures.state()

        val actual = RecordingSetupReducer.reduce(
            givenState,
            RecordingSetupIntent.SetDuration(-1),
        )

        assertEquals(0, actual.durationSeconds)
    }

    @Test
    fun `Given a picker draft When setup loads Then it receives every recording detail`() {
        val givenDraft = RecordingDraft(
            selectedSensorIds = setOf(1, 2),
            includesGps = true,
            selectedWatchSensorIds = setOf(21),
            customMeasurementName = "Morning walk",
            notes = "Hill route",
        )

        val actual = RecordingSetupReducer.reduce(
            RecordingStateFixtures.state(),
            RecordingSetupIntent.LoadDraft(givenDraft),
        )

        assertEquals(givenDraft, actual.toDraft())
    }
}
