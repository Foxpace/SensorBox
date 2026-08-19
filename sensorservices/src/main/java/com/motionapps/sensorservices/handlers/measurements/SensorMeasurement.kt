package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.SensorManager
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.types.SensorHolder
import com.motionapps.sensorservices.types.SensorSpec

class SensorMeasurement {
    private val holders = mutableListOf<SensorHolder>()
    private var samplingPeriod = SensorManager.SENSOR_DELAY_FASTEST

    fun prepare(
        context: Context,
        folderName: String,
        useInternalStorage: Boolean,
        sensorTypes: Set<Int>,
        samplingPeriod: Int,
    ): AppResult<Unit> {
        this.samplingPeriod = samplingPeriod
        return sensorTypes.sorted().fold(
            AppResult.success(Unit),
        ) { result, sensorType ->
            result.flatMap {
                createHolder(context, folderName, useInternalStorage, sensorType).map { holder ->
                    holder?.let(holders::add)
                    Unit
                }
            }
        }
            .withAppError(AppErrorCode.MEASUREMENT, "Initialize sensors")
    }

    private fun createHolder(
        context: Context,
        folderName: String,
        useInternalStorage: Boolean,
        sensorType: Int,
    ): AppResult<SensorHolder?> {
        val spec = SensorSpec.fromType(sensorType) ?: return AppResult.success(null)
        val stream = if (useInternalStorage) {
            StorageHandler.createFileInInternalFolder(
                context,
                folderName,
                spec.fileName,
            )
        } else {
            StorageHandler.createFileInFolder(
                context,
                folderName,
                "text/csv",
                spec.fileName,
            )
        }
        return stream.map { SensorHolder(spec, it) }
    }

    fun start(context: Context): AppResult<Unit> = appResult(
        AppErrorCode.MEASUREMENT,
        "Access sensor manager",
    ) {
        context.getSystemService(SensorManager::class.java)
    }.flatMap { sensorManager ->
        holders.fold(AppResult.success(Unit)) { result, holder ->
            result.flatMap {
                val sensor = sensorManager.getDefaultSensor(holder.spec.type)
                    ?: return@flatMap AppResult.failure(
                        AppError(AppErrorCode.MEASUREMENT, "Find sensor ${holder.spec.type}"),
                    )
                appResult(AppErrorCode.MEASUREMENT, "Register sensor ${holder.spec.type}") {
                    sensorManager.registerListener(holder, sensor, samplingPeriod)
                }.flatMap { registered ->
                    if (registered) {
                        AppResult.success(Unit)
                    } else {
                        AppResult.failure(
                            AppError(AppErrorCode.MEASUREMENT, "Register sensor ${holder.spec.type}"),
                        )
                    }
                }
            }
        }
    }.withAppError(AppErrorCode.MEASUREMENT, "Start sensors")

    private fun pause(context: Context): AppResult<Unit> = appResult(
        AppErrorCode.MEASUREMENT,
        "Pause sensors",
    ) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        holders.forEach(sensorManager::unregisterListener)
    }

    private suspend fun save(): AppResult<Unit> {
        val results = holders.map { it.close() }
        holders.clear()
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Save sensors")
    }

    suspend fun stop(context: Context): AppResult<Unit> = listOf(
        pause(context),
        save(),
    ).combineAppResults(AppErrorCode.MEASUREMENT, "Stop sensors")
}
