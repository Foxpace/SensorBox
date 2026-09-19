package com.tomasrepcik.sensorbox.core.storage

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import java.io.InputStream
import java.util.UUID

internal class NativeMeasurementImport(
    private val context: Context,
    private val root: DocumentFile,
    private val measurementName: String,
) : MeasurementImport {
    private val staged = checkNotNull(root.createDirectory("$PENDING_MEASUREMENT_PREFIX${UUID.randomUUID()}"))
    private var published = false

    @Synchronized
    override fun copy(fileName: String, mimeType: String, input: InputStream): AppResult<Unit> =
        appResult(AppErrorCode.STORAGE, "Stage watch file") {
            check(!published)
            val previous = staged.findFile(fileName)
            check(previous == null || previous.delete()) { "Could not replace staged file" }
            val file = checkNotNull(staged.createFile(mimeType, fileName))
            checkNotNull(context.contentResolver.openOutputStream(file.uri, "wt")).use { output ->
                input.copyTo(output)
            }
        }

    @Synchronized
    override fun commit(expectedFingerprint: String): AppResult<Unit> =
        appResult(AppErrorCode.STORAGE, "Publish complete watch measurement") {
            check(!published)
            check(fingerprint(staged) == expectedFingerprint) { "Watch measurement is incomplete or changed" }
            val existing = root.findFile(measurementName)
            if (existing != null) {
                check(existing.isDirectory) { "Measurement destination is not a directory" }
                if (fingerprint(existing) == expectedFingerprint) {
                    check(staged.delete()) { "Could not remove duplicate staged measurement" }
                } else {
                    replace(existing)
                }
            } else {
                check(staged.renameTo(measurementName)) { "Archive does not support publishing a complete folder" }
            }
            published = true
        }

    @Synchronized
    override fun discard(): AppResult<Unit> = appResult(AppErrorCode.STORAGE, "Discard incomplete watch measurement") {
        check(published || !staged.exists() || staged.delete()) { "Could not remove incomplete watch measurement" }
    }

    private fun replace(existing: DocumentFile) {
        check(existing.renameTo("$BACKUP_MEASUREMENT_PREFIX$measurementName")) {
            "Could not preserve the previous measurement"
        }
        if (!staged.renameTo(measurementName)) {
            check(existing.renameTo(measurementName)) { "Previous measurement awaits recovery on the next sync" }
            error("Could not publish complete measurement")
        }
        published = true
        // Failure to remove the backup is recoverable on the next sync.
        appResult(AppErrorCode.STORAGE, "Remove previous measurement backup") { existing.delete() }
    }

    private fun fingerprint(directory: DocumentFile): String = measurementFingerprint(
        directory.listFiles().map { file ->
            check(file.isFile) { "Unexpected directory in watch measurement" }
            checkNotNull(file.name) to { checkNotNull(context.contentResolver.openInputStream(file.uri)) }
        },
    )
}
