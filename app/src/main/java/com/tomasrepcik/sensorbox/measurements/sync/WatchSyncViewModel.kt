package com.tomasrepcik.sensorbox.measurements.sync

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.bootstrap.IoDispatcher
import com.tomasrepcik.sensorbox.core.storage.DocumentStorage
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileTransferClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal enum class WatchSyncIntent { CHECK, COPY, RETRY }

@HiltViewModel
class WatchSyncViewModel @Inject constructor(
    private val repo: WatchSyncRepo,
    private val sendCommand: SendWearCommandUseCase,
    private val storage: DocumentStorage,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val transfers: WearFileTransferClient,
    private val destination: WatchFileDestination,
) : ViewModel() {
    private var activeRequestId: String? = null
    internal val state = repo.state

    internal fun accept(intent: WatchSyncIntent) {
        if (state.value.busy) return
        val copy = intent == WatchSyncIntent.COPY || (intent == WatchSyncIntent.RETRY && state.value.retryCopy)
        val requestId = UUID.randomUUID().toString()
        activeRequestId = requestId
        repo.begin(requestId, copy)
        viewModelScope.launch { runRequest(requestId, copy) }
    }

    private suspend fun runRequest(requestId: String, copy: Boolean) {
        var requested = false
        try {
            if (copy && !withContext(ioDispatcher) { storage.hasConfiguredDirectory().getOrNull() == true }) {
                repo.fail("Choose a recording folder in Settings before syncing.", requestId)
                return
            }
            requested = true
            requestMeasurements(requestId, copy)
        } finally {
            if (copy) finishTransfer(requestId, requested)
        }
    }

    private suspend fun finishTransfer(requestId: String, requested: Boolean) {
        transfers.cancel(requestId)
        withContext(NonCancellable + ioDispatcher) {
            val completed = state.value.requestId == requestId && state.value.status == WatchSyncStatus.COMPLETE
            if (!completed) cancelWatch(requestId, requested)
            destination.discard(requestId)
        }
    }

    private suspend fun cancelWatch(requestId: String, requested: Boolean) {
        if (state.value.requestId == requestId && state.value.busy) {
            repo.fail("Sync was interrupted. Try again.", requestId)
        }
        if (requested) {
            withTimeoutOrNull(5_000L.milliseconds) {
                sendCommand(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, WearCommand.CancelWatchSync(requestId))
            }
        }
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        if (state.value.busy && state.value.requestId == activeRequestId) {
            activeRequestId?.let(transfers::cancel)
            repo.fail("Sync was interrupted. Open watch sync to try again.", activeRequestId)
        }
        super.onCleared()
    }

    private suspend fun requestMeasurements(requestId: String, copy: Boolean) {
        val command = if (copy) {
            WearCommand.CopyWatchMeasurements(requestId)
        } else {
            WearCommand.CheckWatchMeasurements(requestId)
        }
        val finished = withTimeoutOrNull((if (copy) COPY_TIMEOUT_MILLIS else CHECK_TIMEOUT_MILLIS).milliseconds) {
            if (sendCommand(WEAR_APP_CAPABILITY, WEAR_MESSAGE_PATH, command).isFailure) {
                repo.fail("Watch unavailable. Connect your watch and try again.", requestId)
            }
            state.first { it.requestId != requestId || !it.busy }
        }
        if (finished == null) repo.fail("Sync timed out. Check the watch connection and try again.", requestId)
    }

    private companion object {
        const val CHECK_TIMEOUT_MILLIS = 15_000L
        const val COPY_TIMEOUT_MILLIS = 300_000L
    }
}
