package com.tomasrepcik.sensorbox.recordinghost.sources.significantmotion

import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.failure.combineAppResults
import com.tomasrepcik.sensorbox.core.failure.flatMap
import com.tomasrepcik.sensorbox.core.failure.withAppError
import com.tomasrepcik.sensorbox.recordinghost.storage.MeasurementStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.OutputStream

/** Handles Android's one-shot significant-motion trigger and re-arms it after every event. */
internal class SignificantMotionRecording(
    private val storage: MeasurementStorage,
    private val sensorManager: SensorManager,
) : TriggerEventListener() {
    private var sensor: Sensor? = null
    private var output: OutputStream? = null
    private var writeFailure: AppError? = null
    private val mutableFailures = MutableSharedFlow<AppError>(replay = 1)

    val failures: Flow<AppError> = mutableFailures.asSharedFlow()

    fun start(folderName: String, useInternalStorage: Boolean): AppResult<Unit> =
        openOutput(folderName, useInternalStorage).flatMap {
            if (arm()) {
                AppResult.success(Unit)
            } else {
                AppResult.failure(AppError(AppErrorCode.RECORDING, "Start significant motion"))
            }
        }

    private fun openOutput(folderName: String, useInternalStorage: Boolean): AppResult<Unit> {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
        val stream = storage.openMeasurementFile(
            folderName = folderName,
            mimeType = "text/csv",
            fileName = FILE_NAME,
            useInternalStorage = useInternalStorage,
        )
        return stream.flatMap { opened ->
            output = opened
            appResult(AppErrorCode.STORAGE, "Write significant motion header") {
                opened.write("t_sensor;event\n".toByteArray())
            }
        }.withAppError(AppErrorCode.RECORDING, "Initialize significant motion")
    }

    private fun pause(): AppResult<Unit> = appResult(
        AppErrorCode.RECORDING,
        "Pause significant motion",
    ) {
        sensor?.let { sensorManager.cancelTriggerSensor(this, it) }
    }

    private fun save(): AppResult<Unit> {
        val results = mutableListOf<AppResult<*>>()
        results += appResult(AppErrorCode.STORAGE, "Close significant motion") { output?.close() }
        writeFailure?.let { results += AppResult.failure(it) }
        output = null
        writeFailure = null
        return results.combineAppResults(AppErrorCode.RECORDING, "Save significant motion")
    }

    suspend fun stop(): AppResult<Unit> {
        val results = listOf(pause(), save())
        sensor = null
        return results.combineAppResults(AppErrorCode.RECORDING, "Stop significant motion")
    }

    override fun onTrigger(event: TriggerEvent?) {
        event?.values?.firstOrNull()?.let { value ->
            appResult(AppErrorCode.STORAGE, "Write significant motion") {
                output?.write("${event.timestamp};$value\n".toByteArray())
            }.onFailure(::reportFailure)
        }
        if (!arm()) reportFailure(AppError(AppErrorCode.RECORDING, "Re-arm significant motion"))
    }

    private fun reportFailure(error: AppError) {
        if (writeFailure != null) return
        writeFailure = error
        mutableFailures.tryEmit(error)
    }

    private fun arm(): Boolean = sensor?.let { sensorManager.requestTriggerSensor(this, it) } == true

    private companion object {
        const val FILE_NAME = "significant_motion.csv"
    }
}
