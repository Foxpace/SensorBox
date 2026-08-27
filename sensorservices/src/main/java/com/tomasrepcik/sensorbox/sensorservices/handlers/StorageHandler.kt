package com.tomasrepcik.sensorbox.sensorservices.handlers

import android.content.Context
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

internal interface MeasurementStorage {
    fun createMeasurementDirectory(folderName: String, useInternalStorage: Boolean): AppResult<Unit>

    fun openMeasurementFile(
        folderName: String,
        mimeType: String,
        fileName: String,
        useInternalStorage: Boolean,
    ): AppResult<OutputStream>
}

/** Storage adapter for files created during a recording session. */
internal class StorageHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentStorage: DocumentStorage,
) : MeasurementStorage {
    override fun createMeasurementDirectory(folderName: String, useInternalStorage: Boolean): AppResult<Unit> =
        if (useInternalStorage) {
            createInternalDirectory(folderName)
        } else {
            documentStorage.createMeasurementDirectory(folderName)
        }

    override fun openMeasurementFile(
        folderName: String,
        mimeType: String,
        fileName: String,
        useInternalStorage: Boolean,
    ): AppResult<OutputStream> {
        if (!useInternalStorage) {
            return documentStorage.openMeasurementFile(folderName, mimeType, fileName)
        }

        return createInternalDirectory(folderName).flatMap {
            appResult(AppErrorCode.STORAGE, "Open internal measurement file") {
                FileOutputStream(File(internalMeasurementDirectory(folderName), fileName))
            }
        }
    }

    private fun createInternalDirectory(folderName: String): AppResult<Unit> = appResult(
        AppErrorCode.STORAGE,
        "Create internal measurement directory",
    ) {
        val directory = internalMeasurementDirectory(folderName)
        check(directory.exists() || directory.mkdirs()) { "Internal measurement directory could not be created" }
    }

    private fun internalMeasurementDirectory(folderName: String): File {
        val appDirectory = File(
            context.filesDir,
            context.getString(com.tomasrepcik.sensorbox.sensorservices.R.string.app_name),
        )
        return File(appDirectory, folderName)
    }
}
