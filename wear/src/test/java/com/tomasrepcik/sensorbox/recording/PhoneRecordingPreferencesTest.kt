package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneRecordingPreferencesTest {
    @Test
    fun `Given different watch preferences When a phone request is applied Then phone recording settings are used`() {
        // Given
        val watch = AppPreferences()
        val request = WearRecordingRequest(
            folderName = "walk",
            sensorIds = listOf(1),
            includesGps = true,
            settings = WearRecordingSettings(3, false, true, 5, 0),
        )

        // When
        val preferences = request.recordingPreferences(watch)

        // Then
        assertEquals(3, preferences.recording.sensorSamplingPeriod)
        assertEquals(false, preferences.recording.stopRecordingOnLowBattery)
        assertEquals(true, preferences.recording.useWakeLock)
        assertEquals(5, preferences.recording.gpsIntervalSeconds)
        assertEquals(0, preferences.recording.gpsMinDistanceMeters)
        assertEquals(watch.display, preferences.display)
        assertEquals(0, watch.recording.sensorSamplingPeriod)
    }
}
