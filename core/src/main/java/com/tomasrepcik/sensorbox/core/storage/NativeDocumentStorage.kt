package com.tomasrepcik.sensorbox.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import java.io.InputStream
import java.io.OutputStream

interface DocumentStorage {
    fun persistRootAccess(uri: String, grantFlags: Int): AppResult<Unit>

    fun hasConfiguredDirectory(): AppResult<Boolean>

    fun displayPath(): AppResult<String?>

    fun createMeasurementDirectory(measurementName: String): AppResult<Unit>

    fun openMeasurementFile(
        measurementName: String,
        mimeType: String,
        fileName: String,
        replaceExisting: Boolean = false,
    ): AppResult<OutputStream>

    fun deleteMeasurement(measurementName: String): AppResult<Unit>

    fun copyToMeasurement(
        input: InputStream,
        measurementName: String,
        fileName: String,
        mimeType: String,
    ): AppResult<Unit>
}

class NativeDocumentStorage(private val context: Context) : DocumentStorage {
    override fun persistRootAccess(uri: String, grantFlags: Int): AppResult<Unit> {
        val selectedUri = Uri.parse(uri)
        val persistedFlags = grantFlags and READ_WRITE_FLAGS
        if (persistedFlags == 0) return storageFailure("Storage permission was not granted")
        return appResult(AppErrorCode.STORAGE, "Persist storage permission") {
            when (persistedFlags) {
                Intent.FLAG_GRANT_READ_URI_PERMISSION -> context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )

                Intent.FLAG_GRANT_WRITE_URI_PERMISSION -> context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )

                else -> context.contentResolver.takePersistableUriPermission(selectedUri, READ_WRITE_FLAGS)
            }
            DocumentFile.fromTreeUri(context, selectedUri)
        }.flatMap { selectedDirectory ->
            if (selectedDirectory?.isDirectory != true) {
                return@flatMap storageFailure("Selected storage location is not a directory")
            }
            releaseOtherRootPermissions(context, selectedUri)
            if (configuredDirectory() == null) {
                storageFailure("Storage directory is unavailable")
            } else {
                AppResult.success(Unit)
            }
        }
    }

    override fun hasConfiguredDirectory(): AppResult<Boolean> = appResult(
        AppErrorCode.STORAGE,
        "Check storage directory",
    ) {
        configuredDirectory()?.exists() == true
    }

    override fun displayPath(): AppResult<String?> = appResult(
        AppErrorCode.STORAGE,
        "Read storage path",
    ) {
        val selectedDirectory = configuredDirectory() ?: return@appResult null
        selectedDirectory.name ?: selectedDirectory.uri.lastPathSegment
    }

    override fun createMeasurementDirectory(measurementName: String): AppResult<Unit> =
        appResult(AppErrorCode.STORAGE, "Access measurement root") {
            configuredDirectory()
        }.flatMap { appDirectory ->
            if (appDirectory == null) return@flatMap storageFailure("Storage directory is not configured")
            appResult(AppErrorCode.STORAGE, "Create measurement directory") {
                findDirectory(appDirectory, measurementName) != null ||
                    appDirectory.createDirectory(measurementName) != null
            }.flatMap { created ->
                if (created) AppResult.success(Unit) else storageFailure("Unable to create measurement directory")
            }
        }

    override fun openMeasurementFile(
        measurementName: String,
        mimeType: String,
        fileName: String,
        replaceExisting: Boolean,
    ): AppResult<OutputStream> = appResult(AppErrorCode.STORAGE, "Access measurement directory") {
        measurementDirectory(measurementName)
    }.flatMap { directory ->
        if (directory == null) return@flatMap storageFailure("Measurement directory is unavailable")
        createOrReplaceFile(directory, mimeType, fileName, replaceExisting).flatMap { createdFile ->
            if (createdFile == null) return@flatMap storageFailure("Unable to create measurement file")
            appResult(AppErrorCode.STORAGE, "Open measurement file") {
                context.contentResolver.openOutputStream(createdFile.uri, "wt")
            }.flatMap { output ->
                output?.let(AppResult.Companion::success) ?: storageFailure("Unable to open measurement file")
            }
        }
    }

    override fun deleteMeasurement(measurementName: String): AppResult<Unit> =
        appResult(AppErrorCode.STORAGE, "Access measurement directory") {
            configuredDirectory()
        }.flatMap { appDirectory ->
            if (appDirectory == null) return@flatMap storageFailure("Storage directory is not configured")
            val directory = findDirectory(appDirectory, measurementName)
                ?: return@flatMap storageFailure("Measurement does not exist")
            appResult(AppErrorCode.STORAGE, "Delete measurement") { directory.delete() }.flatMap { deleted ->
                if (deleted) AppResult.success(Unit) else storageFailure("Unable to delete measurement")
            }
        }

    override fun copyToMeasurement(
        input: InputStream,
        measurementName: String,
        fileName: String,
        mimeType: String,
    ): AppResult<Unit> = openMeasurementFile(
        measurementName = measurementName,
        mimeType = mimeType,
        fileName = fileName,
        replaceExisting = true,
    ).flatMap { output ->
        appResult(AppErrorCode.STORAGE, "Copy measurement file") {
            input.use { source -> output.use(source::copyTo) }
            Unit
        }
    }

    private fun measurementDirectory(measurementName: String): DocumentFile? {
        val appDirectory = configuredDirectory() ?: return null
        return findDirectory(appDirectory, measurementName)
            ?: appDirectory.createDirectory(measurementName)
    }

    private fun configuredDirectory(): DocumentFile? = persistedRoot()?.takeIf(DocumentFile::isDirectory)

    private fun persistedRoot(): DocumentFile? {
        val permission = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission }
            .maxByOrNull { it.persistedTime }
            ?: return null
        return DocumentFile.fromTreeUri(context, permission.uri)
    }

    private fun findDirectory(parent: DocumentFile, name: String): DocumentFile? =
        parent.findFile(name)?.takeIf(DocumentFile::isDirectory)

    private companion object {
        const val READ_WRITE_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}

private fun createOrReplaceFile(
    directory: DocumentFile,
    mimeType: String,
    fileName: String,
    replaceExisting: Boolean,
): AppResult<DocumentFile?> = appResult(AppErrorCode.STORAGE, "Create measurement file") {
    val existing = directory.findFile(fileName)
    if (replaceExisting && existing != null) {
        if (existing.delete()) directory.createFile(normalizeMimeType(mimeType), fileName) else null
    } else {
        existing ?: directory.createFile(normalizeMimeType(mimeType), fileName)
    }
}

private fun <T> storageFailure(operation: String): AppResult<T> =
    AppResult.failure(AppError(AppErrorCode.STORAGE, operation))

private fun releaseOtherRootPermissions(context: Context, selectedUri: android.net.Uri) {
    val resolver = context.contentResolver
    resolver.persistedUriPermissions
        .filter { it.uri != selectedUri }
        .forEach { permission ->
            val flags =
                (if (permission.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                    (if (permission.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
            if (flags != 0) {
                appResult(AppErrorCode.STORAGE, "Release old storage permission") {
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
