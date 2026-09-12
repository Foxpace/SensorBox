package com.tomasrepcik.sensorbox.measurements.sync

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import com.tomasrepcik.sensorbox.core.storage.MeasurementImport
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileMetadata
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchFileDestination @Inject constructor(
    private val documentStorage: DocumentStorage,
    private val sync: WatchSyncRepo,
) {
    private val pending = mutableMapOf<Pair<String, String>, MeasurementImport>()

    @Synchronized
    fun copy(metadata: WearFileMetadata, input: InputStream): AppResult<Unit> =
        appResult(AppErrorCode.STORAGE, "Receive watch measurement") {
            check(sync.accepts(metadata.requestId)) { "Watch sync request expired" }
            val key = metadata.requestId to metadata.measurementName
            if (metadata.isCommit) {
                val bytes = ByteArray(64)
                java.io.DataInputStream(input).readFully(bytes)
                check(input.read() == -1) { "Invalid measurement commit length" }
                val fingerprint = bytes.decodeToString()
                check(fingerprint.matches(Regex("[a-f0-9]{64}"))) { "Invalid measurement fingerprint" }
                val staged = checkNotNull(pending[key]) { "No staged watch measurement" }
                sync.syncLock.whileActive(metadata.requestId) {
                    check(sync.accepts(metadata.requestId))
                    staged.commit(fingerprint).getOrThrow()
                }
                pending.remove(key)
            } else {
                val staged = pending.getOrPut(key) {
                    documentStorage.beginMeasurementImport("WEAR_${metadata.measurementName}").getOrThrow()
                }
                staged.copy(metadata.fileName, mimeType(metadata.fileName), input).getOrThrow()
            }
        }

    @Synchronized
    fun discard(requestId: String) {
        pending.keys.filter { it.first == requestId }.forEach { key -> pending.remove(key)?.discard() }
    }

    private fun mimeType(fileName: String): String = when (fileName.substringAfterLast('.').lowercase()) {
        "csv" -> "text/csv"
        "json" -> "application/json"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }
}
