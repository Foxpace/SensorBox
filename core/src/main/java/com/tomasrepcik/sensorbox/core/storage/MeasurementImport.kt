package com.tomasrepcik.sensorbox.core.storage

import com.tomasrepcik.sensorbox.core.failure.AppResult
import java.io.InputStream

/** A hidden measurement pinned to one archive until committed or discarded. */
interface MeasurementImport {
    fun copy(fileName: String, mimeType: String, input: InputStream): AppResult<Unit>
    fun commit(expectedFingerprint: String): AppResult<Unit>
    fun discard(): AppResult<Unit>
}

const val PENDING_MEASUREMENT_PREFIX = ".sensorbox-sync-"

const val BACKUP_MEASUREMENT_PREFIX = ".sensorbox-backup-"
