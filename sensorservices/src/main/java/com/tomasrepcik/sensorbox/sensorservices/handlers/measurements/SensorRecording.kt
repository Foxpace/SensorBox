package com.tomasrepcik.sensorbox.sensorservices.handlers.measurements

import android.hardware.SensorManager
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.core.error.withAppError
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.types.SensorFileStats
import com.tomasrepcik.sensorbox.sensorservices.types.SensorHolder
import com.tomasrepcik.sensorbox.sensorservices.types.SensorSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

internal class SensorRecording(
    private val storage: MeasurementStorage,
    private val diagnosticLogger: DiagnosticLogger,
    private val sensorManager: SensorManager? = null,
    private val onStopped: (List<SensorFileStats>) -> Unit = {},
) {
    private val holders = mutableListOf<SensorHolder>()
    private val mutableFailures = MutableSharedFlow<AppError>(replay = 1)

    val failures: Flow<AppError> = mutableFailures.asSharedFlow()

    fun start(
        folderName: String,
        useInternalStorage: Boolean,
        sensorTypes: Set<Int>,
        samplingPeriod: Int,
    ): AppResult<Unit> = openFiles(folderName, useInternalStorage, sensorTypes).flatMap {
        registerSensors(samplingPeriod)
    }

    private fun openFiles(folderName: String, useInternalStorage: Boolean, sensorTypes: Set<Int>): AppResult<Unit> {
        for (sensorType in sensorTypes.sorted()) {
            val holderResult = createHolder(folderName, useInternalStorage, sensorType)
            if (holderResult.isFailure) {
                return AppResult.failure(checkNotNull(holderResult.errorOrNull()))
                    .withAppError(AppErrorCode.RECORDING, "Initialize sensors")
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
        ).map { output ->
            SensorHolder(spec, output, diagnosticLogger) { error -> mutableFailures.tryEmit(error) }
        }
    }

    private fun registerSensors(samplingPeriod: Int): AppResult<Unit> {
        val sensorManager = sensorManager
            ?: return AppResult.failure(AppError(AppErrorCode.RECORDING, "Access sensor manager"))
        for (holder in holders) {
            val registration = registerHolder(sensorManager, holder, samplingPeriod)
            if (registration.isFailure) {
                return registration.withAppError(AppErrorCode.RECORDING, "Start sensors")
            }
        }
        return AppResult.success(Unit)
    }

    private fun registerHolder(
        sensorManager: SensorManager,
        holder: SensorHolder,
        samplingPeriod: Int,
    ): AppResult<Unit> {
        val sensor = sensorManager.getDefaultSensor(holder.spec.type)
            ?: return AppResult.failure(AppError(AppErrorCode.RECORDING, "Find sensor ${holder.spec.type}"))
        val registration = appResult(AppErrorCode.RECORDING, "Register sensor ${holder.spec.type}") {
            sensorManager.registerListener(holder, sensor, samplingPeriod)
        }
        return if (registration.getOrNull() == true) {
            AppResult.success(Unit)
        } else {
            AppResult.failure(
                registration.errorOrNull()
                    ?: AppError(AppErrorCode.RECORDING, "Register sensor ${holder.spec.type}"),
            )
        }
    }

    private fun pause(): AppResult<Unit> = appResult(AppErrorCode.RECORDING, "Pause sensors") {
        sensorManager?.let { manager -> holders.forEach(manager::unregisterListener) }
    }

    private suspend fun save(): AppResult<Unit> {
        val results = holders.map { it.close() }
        onStopped(holders.map(SensorHolder::stats))
        holders.clear()
        return results.combineAppResults(AppErrorCode.RECORDING, "Save sensors")
    }

    suspend fun stop(): AppResult<Unit> = listOf(
        pause(),
        save(),
    ).combineAppResults(AppErrorCode.RECORDING, "Stop sensors")
}
