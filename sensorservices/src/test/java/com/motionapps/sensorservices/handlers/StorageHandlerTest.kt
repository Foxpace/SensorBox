package com.motionapps.sensorservices.handlers

import android.content.ContextWrapper
import android.content.Intent
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class StorageHandlerTest {
    @Test
    fun `Given configured storage When session directory and file are requested Then production adapter delegates`() {
        val documents = FakeDocumentStorage()
        val storage: MeasurementStorage = StorageHandler(ContextWrapper(null), documents)

        val directory = storage.createMeasurementDirectory("session", useInternalStorage = false)
        val file = storage.openMeasurementFile(
            folderName = "session",
            mimeType = "text/csv",
            fileName = "sensor.csv",
            useInternalStorage = false,
        )

        assertTrue(directory.isSuccess)
        assertSame(documents.output, file.getOrNull())
        assertEquals("session", documents.createdMeasurement)
        assertEquals("sensor.csv", documents.openedFile)
    }

    private class FakeDocumentStorage : DocumentStorage {
        val output = ByteArrayOutputStream()
        var createdMeasurement: String? = null
        var openedFile: String? = null

        override fun persistRootAccess(intent: Intent): AppResult<Unit> = AppResult.success(Unit)

        override fun hasConfiguredDirectory(): AppResult<Boolean> = AppResult.success(true)

        override fun displayPath(): AppResult<String?> = AppResult.success("fixture")

        override fun createMeasurementDirectory(measurementName: String): AppResult<Unit> {
            createdMeasurement = measurementName
            return AppResult.success(Unit)
        }

        override fun openMeasurementFile(
            measurementName: String,
            mimeType: String,
            fileName: String,
            replaceExisting: Boolean,
        ): AppResult<OutputStream> {
            openedFile = fileName
            return AppResult.success(output)
        }

        override fun deleteMeasurement(measurementName: String): AppResult<Unit> = AppResult.success(Unit)

        override fun copyToMeasurement(
            input: InputStream,
            measurementName: String,
            fileName: String,
            mimeType: String,
        ): AppResult<Unit> = AppResult.success(Unit)
    }
}
