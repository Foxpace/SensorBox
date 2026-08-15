package com.motionapps.sensorservices.handlers

import android.content.Context
import android.content.Intent
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.storage.NativeDocumentStorage
import com.motionapps.sensorservices.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Native storage operations shared by the foreground measurement service. */
object StorageHandler {
    fun getDate(milliseconds: Long, format: String = "dd. MM. yyyy HH:mm:ss"): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = milliseconds }
        return SimpleDateFormat(format, Locale.getDefault()).format(calendar.time)
    }

    fun createMainFolder(context: Context, intent: Intent?): Result<Unit> {
        val directoryName = context.getString(R.string.app_name)
        return if (intent == null) {
            NativeDocumentStorage.hasAppDirectory(context, directoryName).flatMap { exists ->
                if (exists) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        AppError(AppError.Kind.STORAGE, "Storage directory is not configured"),
                    )
                }
            }
        } else {
            NativeDocumentStorage.persistRootAccess(context, intent, directoryName)
        }
    }

    fun isFolder(context: Context): Result<Boolean> = NativeDocumentStorage.hasAppDirectory(
        context = context,
        appDirectoryName = context.getString(R.string.app_name),
    )

    fun isAccess(context: Context): Result<Boolean> = isFolder(context)

    fun getFolderName(context: Context): Result<String> = NativeDocumentStorage.displayPath(
        context = context,
        appDirectoryName = context.getString(R.string.app_name),
    ).map { it ?: context.getString(R.string.no_path) }

    fun createInternalStorageMeasurementFolder(context: Context, folderName: String): Result<Unit> {
        val directory = internalMeasurementDirectory(context, folderName)
        return appResult(AppError.Kind.STORAGE, "Create internal measurement directory") {
            directory.exists() || directory.mkdirs()
        }.flatMap { created ->
            if (created) {
                Result.success(Unit)
            } else {
                Result.failure(
                    AppError(AppError.Kind.STORAGE, "Create internal measurement directory"),
                )
            }
        }
    }

    fun createFolderMeasurement(context: Context, folderName: String): Result<Unit> =
        NativeDocumentStorage.createMeasurementDirectory(
            context = context,
            appDirectoryName = context.getString(R.string.app_name),
            measurementName = folderName,
        )

    fun createFileInFolder(
        context: Context,
        folderName: String,
        mimeOfNewFile: String,
        nameOfNewFile: String,
    ): Result<OutputStream> = NativeDocumentStorage.openMeasurementFile(
        context = context,
        appDirectoryName = context.getString(R.string.app_name),
        measurementName = folderName,
        mimeType = mimeOfNewFile,
        fileName = nameOfNewFile,
    )

    fun createFileInInternalFolder(context: Context, folderName: String, nameOfFile: String): Result<OutputStream> =
        appResult(AppError.Kind.STORAGE, "Prepare internal measurement directory") {
            val directory = internalMeasurementDirectory(context, folderName)
            directory to (directory.exists() || directory.mkdirs())
        }.flatMap { (directory, ready) ->
            if (!ready) {
                Result.failure(
                    AppError(AppError.Kind.STORAGE, "Create internal measurement directory"),
                )
            } else {
                appResult(AppError.Kind.STORAGE, "Open internal measurement file") {
                    FileOutputStream(File(directory, nameOfFile))
                }
            }
        }

    fun deleteByNameOfFolder(context: Context, deleteFolder: String): Result<Unit> =
        NativeDocumentStorage.deleteMeasurement(
            context = context,
            appDirectoryName = context.getString(R.string.app_name),
            measurementName = deleteFolder,
        )

    private fun internalMeasurementDirectory(context: Context, folderName: String): File {
        val appDirectory = File(context.filesDir, context.getString(R.string.app_name))
        return File(appDirectory, folderName)
    }
}
