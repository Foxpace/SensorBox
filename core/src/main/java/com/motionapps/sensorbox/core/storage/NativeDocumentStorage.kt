package com.motionapps.sensorbox.core.storage

import android.content.Context
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import java.io.InputStream
import java.io.OutputStream

object NativeDocumentStorage {
    fun persistRootAccess(context: Context, intent: Intent, appDirectoryName: String): Result<Unit> {
        val uri = intent.data ?: return storageFailure("Storage directory was not selected")
        val grantFlags = intent.flags and READ_WRITE_FLAGS
        if (grantFlags == 0) return storageFailure("Storage permission was not granted")
        return appResult(AppError.Kind.STORAGE, "Persist storage permission") {
            context.contentResolver.takePersistableUriPermission(uri, grantFlags)
            DocumentFile.fromTreeUri(context, uri)
        }.flatMap { selectedDirectory ->
            if (selectedDirectory?.isDirectory != true) {
                return@flatMap storageFailure("Selected storage location is not a directory")
            }
            releaseOtherRootPermissions(context, uri)
            if (appDirectory(context, appDirectoryName, create = true) == null) {
                storageFailure("Storage directory is unavailable")
            } else {
                Result.success(Unit)
            }
        }
    }

    fun hasAppDirectory(context: Context, appDirectoryName: String): Result<Boolean> = appResult(
        AppError.Kind.STORAGE,
        "Check storage directory",
    ) {
        appDirectory(context, appDirectoryName, create = false)?.exists() == true
    }

    fun displayPath(context: Context, appDirectoryName: String): Result<String?> = appResult(
        AppError.Kind.STORAGE,
        "Read storage path",
    ) {
        val selectedDirectory = appDirectory(context, appDirectoryName, create = false) ?: return@appResult null
        selectedDirectory.name ?: selectedDirectory.uri.lastPathSegment
    }

    fun createMeasurementDirectory(context: Context, appDirectoryName: String, measurementName: String): Result<Unit> =
        appResult(AppError.Kind.STORAGE, "Access measurement root") {
            appDirectory(context, appDirectoryName, create = false)
        }.flatMap { appDirectory ->
            if (appDirectory == null) return@flatMap storageFailure("Storage directory is not configured")
            appResult(AppError.Kind.STORAGE, "Create measurement directory") {
                findDirectory(appDirectory, measurementName) != null ||
                    appDirectory.createDirectory(measurementName) != null
            }.flatMap { created ->
                if (created) Result.success(Unit) else storageFailure("Unable to create measurement directory")
            }
        }

    fun openMeasurementFile(
        context: Context,
        appDirectoryName: String,
        measurementName: String,
        mimeType: String,
        fileName: String,
        replaceExisting: Boolean = false,
    ): Result<OutputStream> = appResult(AppError.Kind.STORAGE, "Access measurement directory") {
        measurementDirectory(context, appDirectoryName, measurementName)
    }.flatMap { directory ->
        if (directory == null) return@flatMap storageFailure("Measurement directory is unavailable")
        createOrReplaceFile(directory, mimeType, fileName, replaceExisting).flatMap { createdFile ->
            if (createdFile == null) return@flatMap storageFailure("Unable to create measurement file")
            appResult(AppError.Kind.STORAGE, "Open measurement file") {
                context.contentResolver.openOutputStream(createdFile.uri, "wt")
            }.flatMap { output ->
                output?.let(Result.Companion::success) ?: storageFailure("Unable to open measurement file")
            }
        }
    }

    fun deleteMeasurement(context: Context, appDirectoryName: String, measurementName: String): Result<Unit> =
        appResult(AppError.Kind.STORAGE, "Access measurement directory") {
            appDirectory(context, appDirectoryName, create = false)
        }.flatMap { appDirectory ->
            if (appDirectory == null) return@flatMap storageFailure("Storage directory is not configured")
            val directory = findDirectory(appDirectory, measurementName)
                ?: return@flatMap storageFailure("Measurement does not exist")
            appResult(AppError.Kind.STORAGE, "Delete measurement") { directory.delete() }.flatMap { deleted ->
                if (deleted) Result.success(Unit) else storageFailure("Unable to delete measurement")
            }
        }

    fun copyToMeasurement(
        context: Context,
        input: InputStream,
        appDirectoryName: String,
        measurementName: String,
        fileName: String,
        mimeType: String,
    ): Result<Unit> = openMeasurementFile(
        context = context,
        appDirectoryName = appDirectoryName,
        measurementName = measurementName,
        mimeType = mimeType,
        fileName = fileName,
        replaceExisting = true,
    ).flatMap { output ->
        appResult(AppError.Kind.STORAGE, "Copy measurement file") {
            input.use { source -> output.use(source::copyTo) }
            Unit
        }
    }

    private fun measurementDirectory(
        context: Context,
        appDirectoryName: String,
        measurementName: String,
    ): DocumentFile? {
        val appDirectory = appDirectory(context, appDirectoryName, create = false) ?: return null
        return findDirectory(appDirectory, measurementName)
            ?: appDirectory.createDirectory(measurementName)
    }

    private fun appDirectory(
        context: Context,
        @Suppress("UNUSED_PARAMETER") directoryName: String,
        @Suppress("UNUSED_PARAMETER") create: Boolean,
    ): DocumentFile? = persistedRoot(context)?.takeIf(DocumentFile::isDirectory)

    private fun persistedRoot(context: Context): DocumentFile? {
        val permission = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission }
            .maxByOrNull { it.persistedTime }
            ?: return null
        return DocumentFile.fromTreeUri(context, permission.uri)
    }

    private fun findDirectory(parent: DocumentFile, name: String): DocumentFile? =
        parent.findFile(name)?.takeIf(DocumentFile::isDirectory)

    private const val READ_WRITE_FLAGS =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
}

private fun createOrReplaceFile(
    directory: DocumentFile,
    mimeType: String,
    fileName: String,
    replaceExisting: Boolean,
): Result<DocumentFile?> = appResult(AppError.Kind.STORAGE, "Create measurement file") {
    val existing = directory.findFile(fileName)
    if (replaceExisting && existing != null) {
        if (existing.delete()) directory.createFile(normalizeMimeType(mimeType), fileName) else null
    } else {
        existing ?: directory.createFile(normalizeMimeType(mimeType), fileName)
    }
}

private fun <T> storageFailure(operation: String): Result<T> =
    Result.failure(AppError(AppError.Kind.STORAGE, operation))

private fun releaseOtherRootPermissions(context: Context, selectedUri: android.net.Uri) {
    val resolver = context.contentResolver
    resolver.persistedUriPermissions
        .filter { it.uri != selectedUri }
        .forEach { permission ->
            val flags =
                (if (permission.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                    (if (permission.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
            if (flags != 0) {
                appResult(AppError.Kind.STORAGE, "Release old storage permission") {
                    resolver.releasePersistableUriPermission(permission.uri, flags)
                }
            }
        }
}

private fun normalizeMimeType(value: String): String = when (value.lowercase()) {
    "csv" -> "text/csv"
    "json" -> "application/json"
    "txt" -> "text/plain"
    else -> value.takeIf { '/' in it } ?: "application/octet-stream"
}
