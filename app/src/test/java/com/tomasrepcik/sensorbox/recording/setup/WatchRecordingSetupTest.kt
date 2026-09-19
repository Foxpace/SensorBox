package com.tomasrepcik.sensorbox.recording.setup

import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchRecordingSetupTest {
    @Test
    fun `Given phone setup When sent to the watch Then its sources and recording settings survive transport`() {
        // Given
        val setup = RecordingSetup(
            sensorIds = setOf(2),
            includesGps = false,
            samplingPeriodIndex = 2,
            stopOnLowBattery = false,
            useWakeLock = true,
            gpsIntervalSeconds = 7,
            gpsMinDistanceMeters = 3,
            watchSensorIds = setOf(4),
            watchIncludesGps = true,
        )
        val started = StartedPhoneRecording("phone", "walk", 60_000L)
        val command = WearCommand.StartRecording("phone", started.toWatchRecordingRequest(setup))

        // When
        val payload = checkNotNull(WearCommandCodec.encode(command).getOrNull())
        val received = WearCommandCodec.decode(payload).getOrNull() as WearCommand.StartRecording

        // Then
        assertEquals(listOf(4), received.request.sensorIds)
        assertEquals(true, received.request.includesGps)
        assertEquals(60_000L, received.request.durationMillis)
        assertEquals(WearRecordingSettings(2, false, true, 7, 3), received.request.settings)
    }
}
