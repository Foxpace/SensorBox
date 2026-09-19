package com.tomasrepcik.sensorbox.wearoslib.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearFilePathCodecTest {
    @Test
    fun `Given an empty request identifier When encoded Then the file is rejected`() {
        // Given
        val metadata = WearFileMetadata("walk", "sensor.csv", "")

        // When
        val result = WearFilePathCodec.encode(metadata)

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `Given missing request metadata When decoded Then the file is rejected`() {
        // Given
        val json = """{"measurementName":"walk","fileName":"sensor.csv"}"""
        val encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())

        // When
        val result = WearFilePathCodec.decode("${WearFilePathCodec.PREFIX}/$encoded")

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `Given safe metadata When encoded and decoded Then names survive`() {
        val given = WearFileMetadata("recording_2026-08-13_12-30-00", "accelerometer.csv", "request")

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
