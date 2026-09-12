package com.tomasrepcik.sensorbox.measurements.sync

import com.tomasrepcik.sensorbox.core.storage.measurementFingerprint
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileMetadata
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchFileDestinationTest {
    private val storage = WatchSyncStorageFixture()
    private val sync = WatchSyncRepo(
        com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock(),
    ).apply { begin("request", copy = true) }
    private val destination = WatchFileDestination(storage, sync)
    private val file = WearFileMetadata("walk", "sensor.csv", "request")
    private val bytes = "time,x,y,z\n1,2,3,4".toByteArray()
    private val fingerprint = measurementFingerprint(listOf(file.fileName to bytes::inputStream))
    private val commit = file.copy(fileName = WearFileMetadata.COMMIT_FILE)

    @Test
    fun `Given an active sync When a file arrives Then the archive remains unchanged`() {
        // Given
        storage.savedFiles["WEAR_other/extra.json"] = "{}".toByteArray()

        // When
        val result = destination.copy(file, bytes.inputStream())

        // Then
        assertTrue(result.isSuccess)
        assertEquals(setOf("WEAR_other/extra.json"), storage.savedFiles.keys)
    }

    @Test
    fun `Given a complete staged measurement When the fingerprint matches Then the whole measurement is published`() {
        // Given
        destination.copy(file, bytes.inputStream()).getOrThrow()

        // When
        val result = destination.copy(commit, fingerprint.byteInputStream())

        // Then
        assertTrue(result.isSuccess)
        assertArrayEquals(bytes, storage.savedFiles["WEAR_walk/sensor.csv"])
    }

    @Test
    fun `Given an incomplete measurement When commit arrives Then existing archive data survives`() {
        // Given
        storage.savedFiles["WEAR_walk/sensor.csv"] = bytes
        destination.copy(file, "partial".byteInputStream()).getOrThrow()

        // When
        val result = destination.copy(commit, fingerprint.byteInputStream())

        // Then
        assertTrue(result.isFailure)
        assertArrayEquals(bytes, storage.savedFiles["WEAR_walk/sensor.csv"])
    }

    @Test
    fun `Given a cancelled transfer When its commit arrives during retry Then no old measurement is published`() {
        // Given
        destination.copy(file, bytes.inputStream()).getOrThrow()
        sync.fail("Cancelled", "request")
        sync.begin("retry", copy = true)

        // When
        val result = destination.copy(commit, fingerprint.byteInputStream())
        destination.discard("request")

        // Then
        assertTrue(result.isFailure)
        assertTrue(storage.savedFiles.isEmpty())
        assertEquals(1, storage.discarded)
        assertTrue(sync.accepts("retry"))
    }

    @Test
    fun `Given an acknowledged measurement When retry sends it again Then the existing copy remains valid`() {
        // Given
        destination.copy(file, bytes.inputStream()).getOrThrow()
        destination.copy(commit, fingerprint.byteInputStream()).getOrThrow()

        // When
        destination.copy(file, bytes.inputStream()).getOrThrow()
        val result = destination.copy(commit, fingerprint.byteInputStream())

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, storage.savedFiles.size)
        assertArrayEquals(bytes, storage.savedFiles["WEAR_walk/sensor.csv"])
    }
}
