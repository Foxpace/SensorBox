package com.motionapps.sensorservices.handlers.measurements

import com.motionapps.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.core.error.withAppError
import java.io.OutputStream

/** Records periodic activity-recognition confidence values. */
internal class ActivityRecognitionMeasurement(
    private val periodSeconds: Int,
    private val storage: MeasurementStorage,
    private val platform: ActivityRecognitionPlatform,
) {
    private var updatesOutput: OutputStream? = null
    private var transitionsOutput: OutputStream? = null
    private var writeFailure: AppError? = null

    fun prepare(folderName: String, useInternalStorage: Boolean): AppResult<Unit> {
        val updatesResult = storage.openMeasurementFile(
            folderName = folderName,
            mimeType = "text/csv",
            fileName = UPDATES_FILE_NAME,
            useInternalStorage = useInternalStorage,
        )
        val updates = updatesResult.getOrNull()
            ?: return AppResult.failure(checkNotNull(updatesResult.errorOrNull()))
                .withAppError(AppErrorCode.MEASUREMENT, "Initialize activity recognition")
        updatesOutput = updates

        val transitionsResult = storage.openMeasurementFile(
            folderName = folderName,
            mimeType = "text/csv",
            fileName = TRANSITIONS_FILE_NAME,
            useInternalStorage = useInternalStorage,
        )
        val transitions = transitionsResult.getOrNull()
        if (transitions == null) {
            appResult(AppErrorCode.STORAGE, "Close incomplete activity measurement") { updates.close() }
            updatesOutput = null
            return AppResult.failure(checkNotNull(transitionsResult.errorOrNull()))
                .withAppError(AppErrorCode.MEASUREMENT, "Initialize activity recognition")
        }
        transitionsOutput = transitions

        val headers = appResult(AppErrorCode.STORAGE, "Write activity recognition headers") {
            updates.write(UPDATES_HEADER.toByteArray())
            transitions.write(TRANSITIONS_HEADER.toByteArray())
        }
        return headers.flatMap {
            platform.prepare(::writeActivityUpdate, ::writeActivityTransitions)
        }
            .withAppError(AppErrorCode.MEASUREMENT, "Initialize activity recognition")
    }

    fun start(): AppResult<Unit> = platform.start(periodSeconds)
        .withAppError(AppErrorCode.MEASUREMENT, "Start activity recognition")

    suspend fun stop(): AppResult<Unit> {
        val results = listOf(platform.stop(), save())
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Stop activity recognition")
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
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Save activity recognition")
    }

    private inline fun recordWriteFailure(operation: String, block: () -> Unit) {
        if (writeFailure != null) return
        appResult(AppErrorCode.STORAGE, operation, block).onFailure { writeFailure = it }
    }

    private companion object {
        const val UPDATES_FILE_NAME = "activity_updates.csv"
        const val TRANSITIONS_FILE_NAME = "activity_transitions.csv"
        const val UPDATES_HEADER = "t_elapsed;still;on_foot;walking;running;vehicle;bike;unknown;tilting\n"
        const val TRANSITIONS_HEADER = "t_nanos;activity;enter_exit\n"
    }
}
