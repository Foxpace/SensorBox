package com.tomasrepcik.sensorbox.measurements.sync

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import com.tomasrepcik.sensorbox.core.storage.MeasurementImport
import com.tomasrepcik.sensorbox.core.storage.measurementFingerprint
import java.io.InputStream
import java.io.OutputStream

internal class WatchSyncStorageFixture(var configured: Boolean = true) : DocumentStorage {
    val savedFiles = mutableMapOf<String, ByteArray>()
    var discarded = 0

    override fun beginMeasurementImport(measurementName: String): AppResult<MeasurementImport> =
        AppResult.success(object : MeasurementImport {
            private val staged = mutableMapOf<String, ByteArray>()
            override fun copy(fileName: String, mimeType: String, input: InputStream): AppResult<Unit> =
                appResult(AppErrorCode.STORAGE, "Stage fixture file") { staged[fileName] = input.readBytes() }

            override fun commit(expectedFingerprint: String): AppResult<Unit> =
                appResult(AppErrorCode.STORAGE, "Commit fixture measurement") {
                    check(
                        measurementFingerprint(
                            staged.map { (name, bytes) -> name to bytes::inputStream },
                        ) == expectedFingerprint,
                    )
                    savedFiles.keys.removeAll { it.startsWith("$measurementName/") }
                    staged.forEach { (name, bytes) -> savedFiles["$measurementName/$name"] = bytes }
                }

            override fun discard(): AppResult<Unit> {
                discarded += 1
                staged.clear()
                return AppResult.success(Unit)
            }
        })

    override fun hasConfiguredDirectory(): AppResult<Boolean> = AppResult.success(configured)

    override fun displayPath(): AppResult<String?> = AppResult.success("Documents/Measurements")

    override fun persistRootAccess(uri: String, grantFlags: Int): AppResult<Unit> = error("Not used")

    override fun createMeasurementDirectory(measurementName: String): AppResult<Unit> = error("Not used")

    override fun deleteMeasurement(measurementName: String): AppResult<Unit> = error("Not used")

    override fun openMeasurementFile(
        measurementName: String,
        mimeType: String,
        fileName: String,
    ): AppResult<OutputStream> = error("Not used")
}
