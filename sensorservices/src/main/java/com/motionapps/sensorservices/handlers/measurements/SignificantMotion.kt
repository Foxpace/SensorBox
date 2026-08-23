package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorservices.handlers.StorageHandler
import java.io.OutputStream

/** Handles Android's one-shot significant-motion trigger and re-arms it after every event. */
class SignificantMotion : TriggerEventListener() {
    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private var output: OutputStream? = null
    private var writeFailure: AppError? = null

    fun prepare(context: Context, folderName: String, useInternalStorage: Boolean): AppResult<Unit> {
        sensorManager = context.getSystemService(SensorManager::class.java)
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
        val stream = if (useInternalStorage) {
            StorageHandler.createFileInInternalFolder(context, folderName, FILE_NAME)
        } else {
            StorageHandler.createFileInFolder(context, folderName, "text/csv", FILE_NAME)
        }
        return stream.flatMap { opened ->
            output = opened
            appResult(AppErrorCode.STORAGE, "Write significant motion header") {
                opened.write("t_unix;event\n".toByteArray())
            }
        }.withAppError(AppErrorCode.MEASUREMENT, "Initialize significant motion")
    }

    fun start(): AppResult<Unit> = if (arm()) {
        AppResult.success(Unit)
    } else {
        AppResult.failure(AppError(AppErrorCode.MEASUREMENT, "Start significant motion"))
    }

    private fun pause(): AppResult<Unit> = appResult(
        AppErrorCode.MEASUREMENT,
        "Pause significant motion",
    ) {
        sensor?.let { sensorManager?.cancelTriggerSensor(this, it) }
    }

    private suspend fun save(): AppResult<Unit> {
        val results = mutableListOf<AppResult<*>>()
        results += appResult(AppErrorCode.STORAGE, "Close significant motion") { output?.close() }
        writeFailure?.let { results += AppResult.failure(it) }
        output = null
        writeFailure = null
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Save significant motion")
    }

    suspend fun stop(): AppResult<Unit> {
        val results = listOf(pause(), save())
        sensor = null
        sensorManager = null
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Stop significant motion")
    }

    override fun onTrigger(event: TriggerEvent?) {
        event?.values?.firstOrNull()?.let { value ->
            appResult(AppErrorCode.STORAGE, "Write significant motion") {
                output?.write("${System.currentTimeMillis()};$value\n".toByteArray())
            }.onFailure { writeFailure = it }
        }
        if (!arm()) AppError(AppErrorCode.MEASUREMENT, "Re-arm significant motion")
    }

    private fun arm(): Boolean = sensor?.let { sensorManager?.requestTriggerSensor(this, it) } == true

    private companion object {
        const val FILE_NAME = "significant_motion.csv"
    }
}
