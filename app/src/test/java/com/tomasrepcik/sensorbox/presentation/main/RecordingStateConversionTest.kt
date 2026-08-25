package com.tomasrepcik.sensorbox.presentation.main

import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.core.preferences.RecordingPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingStateConversionTest {
    @Test
    fun `Given recording state input When converted Then request values are normalized`() {
        val state = RecordingState(
            selectedSensorIds = setOf(1),
            includesGps = true,
            customMeasurementName = "walk",
            durationSeconds = -1,
            notes = " first \n\n second ",
            alarmOffsets = "10, bad; -2 30",
            preferences = AppPreferences(
                recording = RecordingPreferences(
                    sensorSamplingPeriod = 3,
                    gpsIntervalSeconds = 7,
                    gpsMinDistanceMeters = 2,
                ),
            ),
        )

        val request = state.toMeasurementRequest()

        assertEquals(setOf(1), request.sensorIds)
        assertEquals(0, request.durationSeconds)
        assertEquals(listOf("first", "second"), request.notes)
        assertEquals(listOf(10, 30), request.alarmOffsetsSeconds)
        assertEquals(3, request.samplingPeriodIndex)
        assertEquals(7, request.gpsIntervalSeconds)
    }
}
