package com.motionapps.wearoslib.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearCommandCodecTest {
    @Test
    fun `Given a measurement command When encoded and decoded Then all fields survive`() {
        val given = WearCommand.StartMeasurement("session", listOf(1, 4, 21), includesGps = true)

        val actual = WearCommandCodec.decode(WearCommandCodec.encode(given).getOrThrow())

        assertEquals(given, actual.getOrThrow())
    }

    @Test
    fun `Given an unknown payload When decoded Then it is rejected`() {
        val invalidPayload = byteArrayOf(1, 2, 3)

        val actual = WearCommandCodec.decode(invalidPayload)

        assertTrue(actual.isFailure)
    }

    @Test
    fun `Given a synchronized measurement When encoded Then schedule survives`() {
        val given = WearCommand.StartMeasurement(
            folderName = "shared_session",
            sensorIds = listOf(1, 21),
            includesGps = false,
            startAtEpochMillis = 1_800_000_000_000L,
            durationMillis = 45_000L,
            measurementType = "TIMED",
        )

        assertEquals(given, WearCommandCodec.decode(WearCommandCodec.encode(given).getOrThrow()).getOrThrow())
    }

    @Test
    fun `Given a Wear sensor catalogue When encoded and decoded Then descriptors survive`() {
        val given = WearCommand.SensorList(
            listOf(WearSensorInfo(21, "Heart rate", "Fixture", isHeartRate = true)),
        )

        assertEquals(given, WearCommandCodec.decode(WearCommandCodec.encode(given).getOrThrow()).getOrThrow())
    }
}
