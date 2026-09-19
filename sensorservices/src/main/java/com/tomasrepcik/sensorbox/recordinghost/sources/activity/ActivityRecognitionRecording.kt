package com.tomasrepcik.sensorbox.recordinghost.sources.activity

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.failure.combineAppResults
import com.tomasrepcik.sensorbox.core.failure.withAppError
import com.tomasrepcik.sensorbox.recordinghost.storage.MeasurementStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.OutputStream

/** Records periodic activity-recognition confidence values. */
internal class ActivityRecognitionRecording(
    private val storage: MeasurementStorage,
    private val platform: ActivityRecognitionPlatform,
) {
    private var updatesOutput: OutputStream? = null
    private var transitionsOutput: OutputStream? = null
    private var writeFailure: AppError? = null
    private val mutableFailures = MutableSharedFlow<AppError>(replay = 1)

    val failures: Flow<AppError> = mutableFailures.asSharedFlow()

    suspend fun start(folderName: String, useInternalStorage: Boolean, periodSeconds: Int): AppResult<Unit> {
        val opened = openOutputs(folderName, useInternalStorage)
        if (opened.isFailure) return opened
        return platform.start(periodSeconds, ::writeActivityUpdate, ::writeActivityTransitions)
            .withAppError(AppErrorCode.RECORDING, "Start activity recognition")
    }

    private fun openOutputs(folderName: String, useInternalStorage: Boolean): AppResult<Unit> {
        val updatesResult = storage.openMeasurementFile(
            folderName = folderName,
            mimeType = "text/csv",
            fileName = UPDATES_FILE_NAME,
            useInternalStorage = useInternalStorage,
        )
        val updates = updatesResult.getOrNull()
            ?: return AppResult.failure(checkNotNull(updatesResult.errorOrNull()))
                .withAppError(AppErrorCode.RECORDING, "Initialize activity recognition")
        updatesOutput = updates

        val transitionsResult = storage.openMeasurementFile(
            folderName = folderName,
            mimeType = "text/csv",
            fileName = TRANSITIONS_FILE_NAME,
            useInternalStorage = useInternalStorage,
        )
        val transitions = transitionsResult.getOrNull()
        if (transitions == null) {
            appResult(AppErrorCode.STORAGE, "Close incomplete activity measurement file") { updates.close() }
            updatesOutput = null
            return AppResult.failure(checkNotNull(transitionsResult.errorOrNull()))
                .withAppError(AppErrorCode.RECORDING, "Initialize activity recognition")
        }
        transitionsOutput = transitions

        val headers = appResult(AppErrorCode.STORAGE, "Write activity recognition headers") {
            updates.write(UPDATES_HEADER.toByteArray())
            transitions.write(TRANSITIONS_HEADER.toByteArray())
        }
        return headers.withAppError(AppErrorCode.RECORDING, "Initialize activity recognition")
    }

    suspend fun stop(): AppResult<Unit> {
        val results = listOf(platform.stop(), save())
        return results.combineAppResults(AppErrorCode.RECORDING, "Stop activity recognition")
    }

    private fun writeActivityUpdate(update: ActivityUpdate) {
        recordWriteFailure("Write activity update") {
            val confidences = update.confidences.joinToString(";")
            updatesOutput?.write("${update.elapsedRealtimeMillis};$confidences\n".toByteArray())
        }
    }

    private fun writeActivityTransitions(transitions: List<ActivityTransitionSample>) {
        recordWriteFailure("Write activity transition") {
            transitions.forEach { transition ->
                transitionsOutput?.write(
                    "${transition.elapsedRealtimeNanos};${transition.activityType};${transition.transitionType}\n"
                        .toByteArray(),
                )
            }
        }
    }

    private suspend fun save(): AppResult<Unit> {
        val results = mutableListOf<AppResult<*>>()
        results += appResult(AppErrorCode.STORAGE, "Close activity updates") { updatesOutput?.close() }
        results += appResult(AppErrorCode.STORAGE, "Close activity transitions") { transitionsOutput?.close() }
        writeFailure?.let { results += AppResult.failure(it) }
        updatesOutput = null
        transitionsOutput = null
        writeFailure = null
        return results.combineAppResults(AppErrorCode.RECORDING, "Save activity recognition")
    }

    private inline fun recordWriteFailure(operation: String, block: () -> Unit) {
        if (writeFailure != null) return
        appResult(AppErrorCode.STORAGE, operation, block).onFailure { error ->
            writeFailure = error
            mutableFailures.tryEmit(error)
        }
    }

    private companion object {
        const val UPDATES_FILE_NAME = "activity_updates.csv"
        const val TRANSITIONS_FILE_NAME = "activity_transitions.csv"
        const val UPDATES_HEADER = "t_elapsed;still;on_foot;walking;running;vehicle;bike;unknown;tilting\n"
        const val TRANSITIONS_HEADER = "t_nanos;activity;enter_exit\n"
    }
}
