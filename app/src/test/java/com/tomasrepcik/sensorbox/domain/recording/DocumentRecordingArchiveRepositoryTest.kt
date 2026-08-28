package com.tomasrepcik.sensorbox.domain.recording

import android.content.Intent
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream

class DocumentRecordingArchiveRepositoryTest {
    @Test
    fun `Given document storage When recording archive operations run Then each result is returned`() {
        val storage = FakeDocumentStorage()
        val repository = DocumentRecordingArchiveRepository(storage)
        val intent = Intent()

        assertTrue(repository.isSelected().getOrNull() == true)
        assertEquals("Documents/SensorBox", repository.path().getOrNull())
        assertTrue(repository.select(intent).isSuccess)
        assertSame(intent, storage.persistedIntent)
    }

    private class FakeDocumentStorage : DocumentStorage {
        var persistedIntent: Intent? = null

        override fun persistRootAccess(intent: Intent): AppResult<Unit> {
            persistedIntent = intent
            return AppResult.success(Unit)
        }

        override fun hasConfiguredDirectory(): AppResult<Boolean> = AppResult.success(true)

        override fun displayPath(): AppResult<String?> = AppResult.success("Documents/SensorBox")

        override fun createMeasurementDirectory(measurementName: String): AppResult<Unit> = AppResult.success(Unit)

        override fun openMeasurementFile(
            measurementName: String,
            mimeType: String,
            fileName: String,
            replaceExisting: Boolean,
        ): AppResult<OutputStream> = error("not used")

        override fun deleteMeasurement(measurementName: String): AppResult<Unit> = AppResult.success(Unit)

        override fun copyToMeasurement(
            input: InputStream,
            measurementName: String,
            fileName: String,
            mimeType: String,
        ): AppResult<Unit> = AppResult.success(Unit)
    }
}
