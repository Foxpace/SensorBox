package com.motionapps.sensorbox.domain.sync

import android.content.Context
import android.content.pm.ApplicationInfo
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.storage.NativeDocumentStorage
import com.motionapps.wearoslib.files.WearFileMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearFileDestination @Inject constructor(@ApplicationContext private val context: Context) {
    fun isReady(): Result<Boolean> = hasConfiguredDirectory().map { configured -> configured || isDebugBuild() }

    fun copy(metadata: WearFileMetadata, input: InputStream): Result<Unit> {
        val measurementName = "WEAR_${metadata.measurementName}"
        val configured = hasConfiguredDirectory().getOrElse { return Result.failure(it) }
        return if (configured) {
            copyToConfiguredDirectory(measurementName, metadata.fileName, input)
        } else {
            copyToDebugDirectory(measurementName, metadata.fileName, input)
        }
    }

    private fun copyToConfiguredDirectory(measurementName: String, fileName: String, input: InputStream): Result<Unit> =
        NativeDocumentStorage.copyToMeasurement(
            context = context,
            input = input,
            appDirectoryName = APP_DIRECTORY,
            measurementName = measurementName,
            fileName = fileName,
            mimeType = mimeType(fileName),
        )

    private fun copyToDebugDirectory(measurementName: String, fileName: String, input: InputStream): Result<Unit> {
        if (!isDebugBuild()) return Result.failure(AppError(AppError.Kind.STORAGE, "Copy debug Wear file"))
        return appResult(AppError.Kind.STORAGE, "Prepare debug Wear directory") {
            val directory = File(context.filesDir, "$APP_DIRECTORY/$measurementName")
            directory to (directory.isDirectory || directory.mkdirs())
        }.flatMap { (directory, ready) ->
            if (!ready) {
                Result.failure(AppError(AppError.Kind.STORAGE, "Prepare debug Wear directory"))
            } else {
                appResult(AppError.Kind.STORAGE, "Copy debug Wear file") {
                    File(directory, fileName).outputStream().use(input::copyTo)
                    Unit
                }
            }
        }
    }

    private fun hasConfiguredDirectory(): Result<Boolean> = NativeDocumentStorage.hasAppDirectory(
        context,
        APP_DIRECTORY,
    )

    private fun isDebugBuild(): Boolean = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    private fun mimeType(fileName: String): String = when (fileName.substringAfterLast('.').lowercase()) {
        "csv" -> "text/csv"
        "json" -> "application/json"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    private companion object {
        const val APP_DIRECTORY = "SensorBox"
    }
}
