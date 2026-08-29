package com.tomasrepcik.sensorbox.wearoslib.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearFilePathCodecTest {
    @Test
    fun `Given safe metadata When encoded and decoded Then names survive`() {
        val given = WearFileMetadata("recording_2026-08-13_12-30-00", "accelerometer.csv")

        val actual = WearFilePathCodec.decode(WearFilePathCodec.encode(given).getOrThrow())

        assertEquals(given, actual.getOrThrow())
    }

    @Test
    fun `Given a traversal path When decoded Then it is rejected`() {
        val unsafePath = "${WearFilePathCodec.PREFIX}/Li4/file"

        val actual = WearFilePathCodec.decode(unsafePath)

        assertTrue(actual.isFailure)
    }
}
