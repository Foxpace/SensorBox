package com.tomasrepcik.sensorbox.recording.setup

import android.hardware.SensorManager
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingSetupTest {
    @Test
    fun `Given a recording setup When converted Then recording request is preserved`() {
        val request = RecordingSetup(
            sensorIds = setOf(1, 4),
            includesGps = true,
            samplingPeriodIndex = 2,
            stopOnLowBattery = true,
            useWakeLock = true,
            gpsIntervalSeconds = 12,
            gpsMinDistanceMeters = 3,
            durationSeconds = 45,
            notes = listOf("note"),
            alarmOffsetsSeconds = listOf(5),
            activityRecognition = true,
            activityRecognitionPeriodSeconds = 60,
            significantMotion = true,
        )

        val launch = request.toRecordingRequest("session-1", "folder")

        assertEquals("session-1", launch.sessionId)
        assertEquals("folder", launch.folderName)
        assertEquals(setOf(1, 4), launch.sensorIds)
        assertEquals(SensorManager.SENSOR_DELAY_UI, launch.sensorSamplingPeriod)
        assertEquals(45_000L, launch.durationMillis)
        assertEquals(listOf("note"), launch.notes)
        assertEquals(listOf(5), launch.alarmOffsetsSeconds)
        assertEquals(true, launch.activityRecognition)
        assertEquals(true, launch.significantMotion)
    }

    @Test
    fun `Given an unknown sampling index When converted Then fastest is used`() {
        val request = RecordingSetup(
            sensorIds = emptySet(),
            includesGps = false,
            samplingPeriodIndex = Int.MAX_VALUE,
            stopOnLowBattery = false,
            useWakeLock = false,
            gpsIntervalSeconds = 1,
            gpsMinDistanceMeters = 0,
        )

        assertEquals(
            SensorManager.SENSOR_DELAY_FASTEST,
            request.toRecordingRequest("session-1", "folder").sensorSamplingPeriod,
        )
    }
}
