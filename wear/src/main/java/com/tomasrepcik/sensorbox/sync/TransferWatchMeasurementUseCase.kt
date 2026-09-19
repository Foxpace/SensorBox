package com.tomasrepcik.sensorbox.sync

import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.storage.measurementFingerprint
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileMetadata
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileTransferClient
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import javax.inject.Inject

class TransferWatchMeasurementUseCase @Inject constructor(
    private val transferClient: WearFileTransferClient,
    private val sessions: RecordingSessionStore,
) {
    private val cleanup = WatchMeasurementCleanup()

    @Suppress("ReturnCount")
    suspend operator fun invoke(nodeId: String, files: List<File>, requestId: String): AppResult<Unit> {
        val folder = checkNotNull(files.first().parentFile)
        if (cleanup.isConfirmed(folder)) {
            return appResult(AppErrorCode.STORAGE, "Resume confirmed watch cleanup") {
                check(!isRecording(folder.name)) { "Measurement is still recording" }
                cleanup.finish(folder)
            }
        }
        val snapshot = appResult(AppErrorCode.STORAGE, "Read watch measurement before transfer") {
            check(!isRecording(folder.name)) { "Measurement is still recording" }
            fingerprint(files)
        }
        if (snapshot.isFailure) return snapshot.map { }
        for (file in files) {
            val result = transferClient.send(
                nodeId,
                WearFileMetadata(folder.name, file.name, requestId),
                file::inputStream,
            )
            if (result.isFailure) return result
        }
        currentCoroutineContext().ensureActive()
        val unchanged = appResult(AppErrorCode.STORAGE, "Verify watch measurement before publishing") {
            check(!isRecording(folder.name)) { "Measurement is still recording" }
            check(fingerprint(files) == snapshot.getOrNull()) { "Measurement changed during transfer" }
        }
        if (unchanged.isFailure) return unchanged
        val committed = transferClient.send(
            nodeId,
            WearFileMetadata(folder.name, WearFileMetadata.COMMIT_FILE, requestId),
        ) { checkNotNull(snapshot.getOrNull()).byteInputStream() }
        if (committed.isFailure) return committed
        currentCoroutineContext().ensureActive()
        return appResult(AppErrorCode.STORAGE, "Remove confirmed watch measurement") {
            check(!isRecording(folder.name)) { "Measurement is still recording" }
            check(fingerprint(files) == snapshot.getOrNull()) { "Measurement changed during transfer" }
            cleanup.confirm(folder, files)
            cleanup.finish(folder)
        }
    }

    private fun fingerprint(files: List<File>): String =
        measurementFingerprint(files.map { it.name to it::inputStream })

    private fun isRecording(folderName: String): Boolean = when (val session = sessions.state.value) {
        is RecordingSessionState.Running -> session.folderName == folderName
        RecordingSessionState.Stopping -> true
        RecordingSessionState.Idle -> false
    }
}
