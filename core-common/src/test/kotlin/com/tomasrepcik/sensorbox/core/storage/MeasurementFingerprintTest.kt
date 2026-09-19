package com.tomasrepcik.sensorbox.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MeasurementFingerprintTest {
    @Test
    fun `Given the same files in different orders When compared Then their fingerprints match`() {
        // Given
        val files = listOf(file("extra.json", "metadata"), file("sensor.csv", "1,2,3"))

        // When
        val original = measurementFingerprint(files)
        val copied = measurementFingerprint(files.reversed())

        // Then
        assertEquals(original, copied)
    }

    @Test
    fun `Given changed data of the same length When compared Then the measurement needs another copy`() {
        // Given
        val original = listOf(file("sensor.csv", "1,2,3"))
        val changed = listOf(file("sensor.csv", "1,2,4"))

        // When
        val oldFingerprint = measurementFingerprint(original)
        val newFingerprint = measurementFingerprint(changed)

        // Then
        assertNotEquals(oldFingerprint, newFingerprint)
    }

    @Test
    fun `Given a missing file When compared Then the measurement needs a copy`() {
        // Given
        val original = listOf(file("sensor.csv", "1,2,3"), file("extra.json", "metadata"))

        // When
        val fingerprint = measurementFingerprint(original)
        val incomplete = measurementFingerprint(original.take(1))

        // Then
        assertNotEquals(fingerprint, incomplete)
    }

    private fun file(name: String, contents: String) = name to { contents.byteInputStream() }
}
