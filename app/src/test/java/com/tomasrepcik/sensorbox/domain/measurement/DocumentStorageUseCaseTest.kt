package com.tomasrepcik.sensorbox.domain.measurement

import android.content.Intent
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.io.OutputStream

class DocumentStorageUseCaseTest {
    @Test
    fun `Given a document storage adapter When storage operations run Then the same interface owns every result`() {
        val storage = FakeDocumentStorage()
        val useCase = DocumentStorageUseCase(storage)
        val intent = Intent()

        assertTrue(useCase.hasStorage().getOrNull() == true)
        assertEquals("Selected folder", useCase.displayPath().getOrNull())
        assertTrue(useCase.persist(intent).isSuccess)
        assertSame(intent, storage.persistedIntent)
    }

    private class FakeDocumentStorage : DocumentStorage {
        var persistedIntent: Intent? = null

        override fun persistRootAccess(intent: Intent): AppResult<Unit> {
            persistedIntent = intent
            return AppResult.success(Unit)
        }

        override fun hasConfiguredDirectory(): AppResult<Boolean> = AppResult.success(true)

        override fun displayPath(): AppResult<String?> = AppResult.success("Selected folder")

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
