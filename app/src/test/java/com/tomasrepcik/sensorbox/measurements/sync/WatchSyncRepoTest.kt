package com.tomasrepcik.sensorbox.measurements.sync

import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchSyncRepoTest {
    @Test
    fun `Given a transfer When the watch finishes before files are saved Then sync stays in progress`() {
        // Given
        val repo = copyingRepo()

        // When
        repo.receive(finished())

        // Then
        assertEquals(WatchSyncStatus.COPYING, repo.state.value.status)
    }

    @Test
    fun `Given a finished sender When the phone saves the last file Then sync is complete`() {
        // Given
        val repo = copyingRepo()
        repo.receive(finished())

        // When
        repo.fileSaved(file)

        // Then
        assertEquals(WatchSyncStatus.COMPLETE, repo.state.value.status)
        assertEquals(listOf("WEAR_walk/accelerometer.csv"), repo.state.value.receivedFiles)
    }

    @Test
    fun `Given a saved file When the sender finishes Then sync is complete`() {
        // Given
        val repo = copyingRepo()
        repo.fileSaved(file)

        // When
        repo.receive(finished())

        // Then
        assertEquals(WatchSyncStatus.COMPLETE, repo.state.value.status)
    }

    @Test
    fun `Given a failed save When the sender finishes Then sync remains failed`() {
        // Given
        val repo = copyingRepo()
        repo.fail("Cannot write to archive")

        // When
        repo.receive(finished())
        repo.fileSaved(file)

        // Then
        assertEquals(WatchSyncStatus.FAILED, repo.state.value.status)
    }

    @Test
    fun `Given a new request When an old response arrives Then it is ignored`() {
        // Given
        val repo = copyingRepo()

        // When
        repo.receive(finished().copy(requestId = "old"))

        // Then
        assertEquals(null, repo.state.value.fileCount)
        assertEquals(false, repo.state.value.senderFinished)
    }

    @Test
    fun `Given a watch without measurements When syncing finishes Then sync is complete`() {
        // Given
        val repo = copyingRepo()

        // When
        repo.receive(finished().copy(fileCount = 0, measurementCount = 0))

        // Then
        assertEquals(WatchSyncStatus.COMPLETE, repo.state.value.status)
    }

    @Test
    fun `Given repeated notifications When a file is saved twice Then it is listed once`() {
        // Given
        val repo = copyingRepo()
        repo.fileSaved(file)

        // When
        repo.fileSaved(file)

        // Then
        assertEquals(1, repo.state.value.receivedFiles.size)
    }

    @Test
    fun `Given multiple files per measurement When availability arrives Then the badge counts measurements`() {
        // Given
        val repo = WatchSyncRepo(com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock())
        repo.begin("check", copy = false)

        // When
        repo.receive(WearCommand.WatchMeasurementsStatus("check", 15, 2))

        // Then
        assertEquals(2, repo.state.value.measurementsToFetch)
    }

    @Test
    fun `Given a completed copy When availability is refreshed Then no stale badge is displayed`() {
        // Given
        val repo = copyingRepo()
        repo.fileSaved(file)
        repo.receive(finished())
        assertEquals(0, repo.state.value.measurementsToFetch)

        // When
        repo.begin("check", copy = false)

        // Then
        assertEquals(0, repo.state.value.measurementsToFetch)
    }

    @Test
    fun `Given a retry When old files and failures arrive Then the retry remains untouched`() {
        // Given
        val repo = copyingRepo()
        repo.fail("Timed out", "sync")
        repo.begin("retry", copy = true)

        // When
        repo.fileSaved(file)
        repo.fail("Old file failed", "sync")
        repo.receive(finished())

        // Then
        assertEquals(WatchSyncStatus.COPYING, repo.state.value.status)
        assertEquals(emptyList<String>(), repo.state.value.receivedFiles)
        assertEquals(true, repo.syncLock.busy.value)
    }

    private fun copyingRepo() = WatchSyncRepo(
        com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock(),
    ).apply { begin("sync", copy = true) }

    private fun finished() = WearCommand.WatchMeasurementsStatus("sync", 1, 1, finished = true)

    private val file = WearFileMetadata("walk", "accelerometer.csv", "sync")
}
