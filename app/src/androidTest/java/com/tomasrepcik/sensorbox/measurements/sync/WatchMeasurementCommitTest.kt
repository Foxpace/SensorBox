package com.tomasrepcik.sensorbox.measurements.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tomasrepcik.sensorbox.core.storage.NativeDocumentStorage
import com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock
import com.tomasrepcik.sensorbox.core.storage.measurementFingerprint
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.io.InputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WatchMeasurementCommitTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val root by lazy { selectedArchive(context) }
    private val name = "sync_test_${UUID.randomUUID()}"
    private val storage = NativeDocumentStorage(context)
    private val bytes = "complete samples".toByteArray()
    private val fingerprint = measurementFingerprint(listOf("sensor.csv" to bytes::inputStream))
    private val imports = mutableListOf<com.tomasrepcik.sensorbox.core.storage.MeasurementImport>()

    @After
    fun cleanFixture() {
        imports.forEach { it.discard() }
        root.findFile(name)?.delete()
    }

    @Test
    fun givenInterruptedFileWhenCopyFailsThenExistingMeasurementRemainsIntact() {
        // Given
        writeFixture(name, "sensor.csv", bytes)
        val pending = stage()
        val interrupted = object : InputStream() {
            override fun read(): Int = throw IOException("Disconnected fixture")
        }

        // When
        val result = pending.copy("sensor.csv", "text/csv", interrupted)

        // Then
        assertTrue(result.isFailure)
        assertArrayEquals(bytes, checkNotNull(root.findFile(name)?.findFile("sensor.csv")).readBytes(context))
    }

    @Test
    fun givenStagedFolderWhenCommittedThenOnlyCompleteReplacementIsPublished() {
        // Given
        writeFixture(name, "old.csv", "old".toByteArray())
        val pending = stage()
        pending.copy("sensor.csv", "text/csv", bytes.inputStream()).getOrThrow()
        assertTrue(root.findFile(name)?.findFile("sensor.csv") == null)

        // When
        pending.commit(fingerprint).getOrThrow()

        // Then
        val published = checkNotNull(root.findFile(name))
        assertEquals(listOf("sensor.csv"), published.listFiles().map { it.name })
        assertArrayEquals(bytes, checkNotNull(published.findFile("sensor.csv")).readBytes(context))
    }

    @Test
    fun givenIncompleteFolderWhenCommitFailsThenNoMeasurementIsPublished() {
        // Given
        val pending = stage()
        pending.copy("sensor.csv", "text/csv", "partial".byteInputStream()).getOrThrow()

        // When
        val result = pending.commit(fingerprint)

        // Then
        assertTrue(result.isFailure)
        assertTrue(root.findFile(name) == null)
    }

    @Test
    fun givenSyncInProgressWhenArchiveSelectionReturnsThenPermissionIsNotChanged() {
        // Given
        val lock = MeasurementSyncLock()
        val guarded = NativeDocumentStorage(context, lock)
        lock.begin("request")

        // When
        val result = guarded.persistRootAccess("invalid-but-never-used", 3)

        // Then
        assertTrue(result.isFailure)
        assertTrue(guarded.hasConfiguredDirectory().getOrThrow())
        lock.finish("request")
        assertFalse(lock.busy.value)
    }

    @Test
    fun givenInterruptedReplacementWhenSyncResumesThenThePreviousFolderIsRecovered() {
        // Given
        val backup = com.tomasrepcik.sensorbox.core.storage.BACKUP_MEASUREMENT_PREFIX + name
        writeFixture(backup, "sensor.csv", bytes)
        val restarted = NativeDocumentStorage(context)

        // When
        val pending = restarted.beginMeasurementImport(name).getOrThrow().also(imports::add)

        // Then
        assertArrayEquals(bytes, checkNotNull(root.findFile(name)?.findFile("sensor.csv")).readBytes(context))
        assertTrue(root.findFile(backup) == null)
        pending.discard().getOrThrow()
    }

    private fun writeFixture(folder: String, file: String, content: ByteArray) {
        storage.openMeasurementFile(folder, "text/csv", file).getOrThrow().use { it.write(content) }
    }

    private fun stage() = storage.beginMeasurementImport(name).getOrThrow().also(imports::add)
}
