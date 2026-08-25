package com.tomasrepcik.sensorbox.domain.sync

import android.content.Context
import android.content.pm.ApplicationInfo
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import com.tomasrepcik.sensorbox.wearoslib.files.WearFileMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearFileDestination @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentStorage: DocumentStorage,
) {
    fun isReady(): AppResult<Boolean> = hasConfiguredDirectory().map { configured -> configured || isDebugBuild() }

    fun copy(metadata: WearFileMetadata, input: InputStream): AppResult<Unit> {
        val measurementName = "WEAR_${metadata.measurementName}"
        val configuredResult = hasConfiguredDirectory()
        val configured = configuredResult.getOrNull() ?: return AppResult.failure(
            checkNotNull(configuredResult.errorOrNull()),
        )
        return if (configured) {
            copyToConfiguredDirectory(measurementName, metadata.fileName, input)
        } else {
            copyToDebugDirectory(measurementName, metadata.fileName, input)
        }
    }

    private fun copyToConfiguredDirectory(
        measurementName: String,
        fileName: String,
        input: InputStream,
    ): AppResult<Unit> = documentStorage.copyToMeasurement(
        input = input,
        measurementName = measurementName,
        fileName = fileName,
        mimeType = mimeType(fileName),
    )

    private fun copyToDebugDirectory(measurementName: String, fileName: String, input: InputStream): AppResult<Unit> {
        if (!isDebugBuild()) return AppResult.failure(AppError(AppErrorCode.STORAGE, "Copy debug Wear file"))
        return appResult(AppErrorCode.STORAGE, "Prepare debug Wear directory") {
            val directory = File(context.filesDir, "$APP_DIRECTORY/$measurementName")
            directory to (directory.isDirectory || directory.mkdirs())
        }.flatMap { (directory, ready) ->
            if (!ready) {
                AppResult.failure(AppError(AppErrorCode.STORAGE, "Prepare debug Wear directory"))
            } else {
                appResult(AppErrorCode.STORAGE, "Copy debug Wear file") {
                    File(directory, fileName).outputStream().use(input::copyTo)
                    Unit
                }
            }
        }
    }

    private fun hasConfiguredDirectory(): AppResult<Boolean> = documentStorage.hasConfiguredDirectory()

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
