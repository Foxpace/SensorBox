package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.types.SensorHolder
import com.motionapps.sensorservices.types.SensorSpec

class SensorMeasurement : MeasurementInterface {
    private val holders = mutableListOf<SensorHolder>()
    private var samplingPeriod = SensorManager.SENSOR_DELAY_FASTEST

    override fun initMeasurement(context: Context, params: Bundle): Result<Unit> {
        samplingPeriod = params.getInt(MeasurementInterface.SENSOR_SPEED)
        return (params.getIntArray(MeasurementInterface.SENSOR_ID) ?: intArrayOf()).fold(
            Result.success(Unit),
        ) { result, sensorType ->
            result.flatMap {
                createHolder(context, params, sensorType).map { holder ->
                    holder?.let(holders::add)
                    Unit
                }
            }
        }
            .withAppError(AppError.Kind.MEASUREMENT, "Initialize sensors")
    }

    private fun createHolder(context: Context, params: Bundle, sensorType: Int): Result<SensorHolder?> {
        val spec = SensorSpec.fromType(sensorType) ?: return Result.success(null)
        val stream = if (params.getBoolean(MeasurementInterface.INTERNAL_STORAGE)) {
            StorageHandler.createFileInInternalFolder(
                context,
                params.getString(MeasurementInterface.FOLDER_NAME).orEmpty(),
                spec.fileName,
            )
        } else {
            StorageHandler.createFileInFolder(
                context,
                params.getString(MeasurementInterface.FOLDER_NAME).orEmpty(),
                "text/csv",
                spec.fileName,
            )
        }
        return stream.map { SensorHolder(spec, it) }
    }

    override fun startMeasurement(context: Context): Result<Unit> = appResult(
        AppError.Kind.MEASUREMENT,
        "Access sensor manager",
    ) {
        context.getSystemService(SensorManager::class.java)
    }.flatMap { sensorManager ->
        holders.fold(Result.success(Unit)) { result, holder ->
            result.flatMap {
                val sensor = sensorManager.getDefaultSensor(holder.spec.type)
                    ?: return@flatMap Result.failure(
                        AppError(AppError.Kind.MEASUREMENT, "Find sensor ${holder.spec.type}"),
                    )
                appResult(AppError.Kind.MEASUREMENT, "Register sensor ${holder.spec.type}") {
                    sensorManager.registerListener(holder, sensor, samplingPeriod)
                }.flatMap { registered ->
                    if (registered) {
                        Result.success(Unit)
                    } else {
                        Result.failure(
                            AppError(AppError.Kind.MEASUREMENT, "Register sensor ${holder.spec.type}"),
                        )
                    }
                }
            }
        }
    }.withAppError(AppError.Kind.MEASUREMENT, "Start sensors")

    override fun pauseMeasurement(context: Context): Result<Unit> = appResult(
        AppError.Kind.MEASUREMENT,
        "Pause sensors",
    ) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        holders.forEach(sensorManager::unregisterListener)
    }

    override suspend fun saveMeasurement(context: Context): Result<Unit> {
        val results = holders.map { it.close() }
        holders.clear()
        return results.combineAppResults(AppError.Kind.MEASUREMENT, "Save sensors")
    }

    override suspend fun onDestroyMeasurement(context: Context): Result<Unit> = listOf(
        pauseMeasurement(context),
        saveMeasurement(context),
    ).combineAppResults(AppError.Kind.MEASUREMENT, "Stop sensors")
}
