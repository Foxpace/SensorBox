package com.tomasrepcik.sensorbox.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.SensorManager
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.withAppError
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.types.SensorHolder
import com.tomasrepcik.sensorbox.sensorservices.types.SensorSpec

internal class SensorMeasurement(
    private val storage: MeasurementStorage,
    private val diagnosticLogger: DiagnosticLogger,
    private val clock: EpochClock,
) {
    private val holders = mutableListOf<SensorHolder>()
    private var samplingPeriod = SensorManager.SENSOR_DELAY_FASTEST

    fun prepare(
        folderName: String,
        useInternalStorage: Boolean,
        sensorTypes: Set<Int>,
        samplingPeriod: Int,
    ): AppResult<Unit> {
        this.samplingPeriod = samplingPeriod
        for (sensorType in sensorTypes.sorted()) {
            val holderResult = createHolder(folderName, useInternalStorage, sensorType)
            if (holderResult.isFailure) {
                return AppResult.failure(checkNotNull(holderResult.errorOrNull()))
                    .withAppError(AppErrorCode.MEASUREMENT, "Initialize sensors")
            }
            holderResult.getOrNull()?.let(holders::add)
        }
        return AppResult.success(Unit)
    }

    private fun createHolder(
        folderName: String,
        useInternalStorage: Boolean,
        sensorType: Int,
    ): AppResult<SensorHolder?> {
        val spec = SensorSpec.fromType(sensorType) ?: return AppResult.success(null)
        return storage.openMeasurementFile(
            folderName = folderName,
            mimeType = "text/csv",
            fileName = spec.fileName,
            useInternalStorage = useInternalStorage,
        ).map { SensorHolder(spec, it, diagnosticLogger, clock) }
    }

    fun start(context: Context): AppResult<Unit> {
        val managerResult = appResult(AppErrorCode.MEASUREMENT, "Access sensor manager") {
            context.getSystemService(SensorManager::class.java)
        }
        val sensorManager = managerResult.getOrNull()
            ?: return AppResult.failure(checkNotNull(managerResult.errorOrNull()))
                .withAppError(AppErrorCode.MEASUREMENT, "Start sensors")
        for (holder in holders) {
            val registration = registerHolder(sensorManager, holder)
            if (registration.isFailure) {
                return registration.withAppError(AppErrorCode.MEASUREMENT, "Start sensors")
            }
        }
        return AppResult.success(Unit)
    }

    private fun registerHolder(sensorManager: SensorManager, holder: SensorHolder): AppResult<Unit> {
        val sensor = sensorManager.getDefaultSensor(holder.spec.type)
            ?: return AppResult.failure(AppError(AppErrorCode.MEASUREMENT, "Find sensor ${holder.spec.type}"))
        val registration = appResult(AppErrorCode.MEASUREMENT, "Register sensor ${holder.spec.type}") {
            sensorManager.registerListener(holder, sensor, samplingPeriod)
        }
        return if (registration.getOrNull() == true) {
            AppResult.success(Unit)
        } else {
            AppResult.failure(
                registration.errorOrNull()
                    ?: AppError(AppErrorCode.MEASUREMENT, "Register sensor ${holder.spec.type}"),
            )
        }
    }

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
