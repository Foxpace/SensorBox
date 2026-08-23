package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.Bundle
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorservices.handlers.StorageHandler
import java.io.OutputStream

/** Handles Android's one-shot significant-motion trigger and re-arms it after every event. */
class SignificantMotion :
    TriggerEventListener(),
    MeasurementInterface {
    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private var output: OutputStream? = null
    private var writeFailure: AppError? = null

    override fun initMeasurement(context: Context, params: Bundle): Result<Unit> {
        sensorManager = context.getSystemService(SensorManager::class.java)
        sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
        val folder = params.getString(MeasurementInterface.FOLDER_NAME).orEmpty()
        val stream = if (params.getBoolean(MeasurementInterface.INTERNAL_STORAGE)) {
            StorageHandler.createFileInInternalFolder(context, folder, FILE_NAME)
        } else {
            StorageHandler.createFileInFolder(context, folder, "text/csv", FILE_NAME)
        }
        return stream.flatMap { opened ->
            output = opened
            appResult(AppError.Kind.STORAGE, "Write significant motion header") {
                opened.write("t_unix;event\n".toByteArray())
            }
        }.withAppError(AppError.Kind.MEASUREMENT, "Initialize significant motion")
    }

    override fun startMeasurement(context: Context): Result<Unit> = if (arm()) {
        Result.success(Unit)
    } else {
        Result.failure(AppError(AppError.Kind.MEASUREMENT, "Start significant motion"))
    }

    override fun pauseMeasurement(context: Context): Result<Unit> = appResult(
        AppError.Kind.MEASUREMENT,
        "Pause significant motion",
    ) {
        sensor?.let { sensorManager?.cancelTriggerSensor(this, it) }
    }

    override suspend fun saveMeasurement(context: Context): Result<Unit> {
        val results = mutableListOf<Result<*>>()
        results += appResult(AppError.Kind.STORAGE, "Close significant motion") { output?.close() }
        writeFailure?.let { results += Result.failure<Unit>(it) }
        output = null
        writeFailure = null
        return results.combineAppResults(AppError.Kind.MEASUREMENT, "Save significant motion")
    }

    override suspend fun onDestroyMeasurement(context: Context): Result<Unit> {
        val results = listOf(pauseMeasurement(context), saveMeasurement(context))
        sensor = null
        sensorManager = null
        return results.combineAppResults(AppError.Kind.MEASUREMENT, "Stop significant motion")
    }

    override fun onTrigger(event: TriggerEvent?) {
        event?.values?.firstOrNull()?.let { value ->
            appResult(AppError.Kind.STORAGE, "Write significant motion") {
                output?.write("${System.currentTimeMillis()};$value\n".toByteArray())
            }.onFailure { writeFailure = it as AppError }
        }
        if (!arm()) AppError(AppError.Kind.MEASUREMENT, "Re-arm significant motion")
    }

    private fun arm(): Boolean = sensor?.let { sensorManager?.requestTriggerSensor(this, it) } == true

    private companion object {
        const val FILE_NAME = "significant_motion.csv"
    }
}
