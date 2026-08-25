package com.motionapps.sensorservices.handlers

import android.content.Context
import com.tomasrepcik.sensorbox.core.error.AppError
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
            val directory = internalMeasurementDirectory(folderName)
            appResult(AppErrorCode.STORAGE, "Create internal measurement directory") {
                directory.exists() || directory.mkdirs()
            }.flatMap { created ->
                if (created) {
                    AppResult.success(Unit)
                } else {
                    AppResult.failure(AppError(AppErrorCode.STORAGE, "Create internal measurement directory"))
                }
            }
        } else {
            documentStorage.createMeasurementDirectory(folderName)
        }

    override fun openMeasurementFile(
        folderName: String,
        mimeType: String,
        fileName: String,
        useInternalStorage: Boolean,
    ): AppResult<OutputStream> = if (useInternalStorage) {
        appResult(AppErrorCode.STORAGE, "Prepare internal measurement directory") {
            val directory = internalMeasurementDirectory(folderName)
            directory to (directory.exists() || directory.mkdirs())
        }.flatMap { (directory, ready) ->
            if (!ready) {
                AppResult.failure(AppError(AppErrorCode.STORAGE, "Create internal measurement directory"))
            } else {
                appResult(AppErrorCode.STORAGE, "Open internal measurement file") {
                    FileOutputStream(File(directory, fileName))
                }
            }
        }
    } else {
        documentStorage.openMeasurementFile(
            measurementName = folderName,
            mimeType = mimeType,
            fileName = fileName,
        )
    }

    private fun internalMeasurementDirectory(folderName: String): File {
        val appDirectory = File(context.filesDir, context.getString(com.motionapps.sensorservices.R.string.app_name))
        return File(appDirectory, folderName)
    }
}
