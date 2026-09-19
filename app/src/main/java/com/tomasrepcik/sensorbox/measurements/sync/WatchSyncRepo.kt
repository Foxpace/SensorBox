package com.tomasrepcik.sensorbox.measurements.sync

import com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

internal enum class WatchSyncStatus { UNKNOWN, CHECKING, AVAILABLE, COPYING, COMPLETE, FAILED }

internal data class WatchSyncState(
    val status: WatchSyncStatus = WatchSyncStatus.UNKNOWN,
    val requestId: String? = null,
    val fileCount: Int? = null,
    val measurementCount: Int = 0,
    val receivedFiles: List<String> = emptyList(),
    val senderFinished: Boolean = false,
    val message: String? = null,
    val retryCopy: Boolean = false,
) {
    val measurementsToFetch: Int get() = if (status == WatchSyncStatus.COMPLETE) 0 else measurementCount
    val busy: Boolean get() = status == WatchSyncStatus.CHECKING || status == WatchSyncStatus.COPYING
}

@Singleton
class WatchSyncRepo @Inject constructor(val syncLock: MeasurementSyncLock) {
    private val mutableState = MutableStateFlow(WatchSyncState())
    internal val state = mutableState.asStateFlow()

    internal fun begin(requestId: String, copy: Boolean) {
        if (copy) syncLock.begin(requestId)
        mutableState.update {
            it.copy(
                status = if (copy) WatchSyncStatus.COPYING else WatchSyncStatus.CHECKING,
                requestId = requestId,
                retryCopy = copy,
                measurementCount = if (!copy && it.status == WatchSyncStatus.COMPLETE) 0 else it.measurementCount,
                receivedFiles = if (copy) emptyList() else it.receivedFiles,
                senderFinished = false,
                message = null,
            )
        }
    }

    fun receive(status: WearCommand.WatchMeasurementsStatus) {
        mutableState.update { current ->
            if (current.requestId != status.requestId || !current.busy) return@update current
            if (status.failed) {
                syncLock.finish(status.requestId)
                return@update current.copy(
                    status = WatchSyncStatus.FAILED,
                    measurementCount = status.measurementCount,
                    message = "The watch could not send its measurements. Try again.",
                )
            }
            current.copy(
                fileCount = status.fileCount,
                measurementCount = status.measurementCount,
                senderFinished = status.finished,
                status = if (status.finished) WatchSyncStatus.COPYING else WatchSyncStatus.AVAILABLE,
            ).completedIfSaved()
        }
    }

    fun fileSaved(metadata: WearFileMetadata) {
        mutableState.update { current ->
            if (!accepts(metadata.requestId) || metadata.isCommit) return@update current
            current.copy(
                receivedFiles = (current.receivedFiles + "WEAR_${metadata.measurementName}/${metadata.fileName}")
                    .distinct(),
            ).completedIfSaved()
        }
    }

    fun accepts(requestId: String): Boolean = requestId.isNotBlank() &&
        state.value.requestId == requestId && state.value.status == WatchSyncStatus.COPYING

    internal fun fail(message: String, requestId: String? = state.value.requestId) {
        mutableState.update {
            if (it.requestId != requestId) it else it.copy(status = WatchSyncStatus.FAILED, message = message)
        }
        requestId?.let(syncLock::finish)
    }

    private fun WatchSyncState.completedIfSaved(): WatchSyncState =
        if (status == WatchSyncStatus.COPYING && senderFinished && receivedFiles.size == fileCount) {
            requestId?.let(syncLock::finish)
            copy(status = WatchSyncStatus.COMPLETE)
        } else {
            this
        }
}
