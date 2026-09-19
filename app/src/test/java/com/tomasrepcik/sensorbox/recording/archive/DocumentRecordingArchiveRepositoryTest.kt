package com.tomasrepcik.sensorbox.recording.archive

import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import com.tomasrepcik.sensorbox.core.storage.MeasurementImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.OutputStream

class DocumentRecordingArchiveRepositoryTest {
    @Test
    fun `Given document storage When recording archive operations run Then each result is returned`() {
        val storage = FakeDocumentStorage()
        val repository = DocumentRecordingArchiveRepository(storage)
        val selection = RecordingArchiveSelection.Selected("content://fixture", 3)

        assertTrue(repository.isSelected().getOrNull() == true)
        assertEquals("Documents/SensorBox", repository.path().getOrNull())
        assertTrue(repository.select(selection).isSuccess)
        assertEquals(selection.uri, storage.persistedUri)
        assertEquals(selection.grantFlags, storage.persistedFlags)
    }

    private class FakeDocumentStorage : DocumentStorage {
        override fun beginMeasurementImport(measurementName: String): AppResult<MeasurementImport> = error("Not used")

        var persistedUri: String? = null
        var persistedFlags: Int? = null

        override fun persistRootAccess(uri: String, grantFlags: Int): AppResult<Unit> {
            persistedUri = uri
            persistedFlags = grantFlags
            return AppResult.success(Unit)
        }

        override fun hasConfiguredDirectory(): AppResult<Boolean> = AppResult.success(true)

        override fun displayPath(): AppResult<String?> = AppResult.success("Documents/SensorBox")

        override fun createMeasurementDirectory(measurementName: String): AppResult<Unit> = AppResult.success(Unit)

        override fun openMeasurementFile(
            measurementName: String,
            mimeType: String,
            fileName: String,
        ): AppResult<OutputStream> = error("not used")

        override fun deleteMeasurement(measurementName: String): AppResult<Unit> = AppResult.success(Unit)
    }
}
