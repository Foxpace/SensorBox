package com.tomasrepcik.sensorbox.sensorservices.handlers.measurements

import android.location.Location
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.core.error.withAppError
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.sensorservices.handlers.GPSHandler
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream

internal class GPSMeasurement(
    private val gpsHandler: GPSHandler,
    private val storage: MeasurementStorage,
    private val clock: EpochClock,
) : GPSHandler.OnLocationChangedCallback {

    private var outputStream: OutputStream? = null
    private var writeFailure: AppError? = null
    private val mutableFailures = MutableSharedFlow<AppError>(replay = 1)

    val failures: Flow<AppError> = mutableFailures.asSharedFlow()

    fun start(
        folderName: String,
        useInternalStorage: Boolean,
        intervalSeconds: Int,
        minimumDistanceMeters: Int,
    ): AppResult<Unit> = openOutput(folderName, useInternalStorage).flatMap {
        gpsHandler.addCallback(this, intervalSeconds, minimumDistanceMeters)
            .withAppError(AppErrorCode.MEASUREMENT, "Start GPS")
    }

    private fun openOutput(folderName: String, useInternalStorage: Boolean): AppResult<Unit> {
        val streamResult = storage.openMeasurementFile(
            folderName = folderName,
            mimeType = "text/csv",
            fileName = "gps.csv",
            useInternalStorage = useInternalStorage,
        )
        val output = streamResult.getOrNull()
            ?: return AppResult.failure(checkNotNull(streamResult.errorOrNull()))
                .withAppError(AppErrorCode.MEASUREMENT, "Initialize GPS measurement")
        outputStream = output
        val headerResult = appResult(AppErrorCode.STORAGE, "Write GPS header") {
            output.write(HEADER.toByteArray())
        }
        return headerResult.withAppError(AppErrorCode.MEASUREMENT, "Initialize GPS measurement")
    }

    private fun locationRow(location: Location): String = "${clock.nowMillis()};" +
        "${location.latitude};" +
        "${location.longitude};" +
        "${location.altitude};" +
        "${location.accuracy};" +
        "${location.speed};" +
        "${location.bearing};" +
        location.provider + "\n"

    private fun pause(): AppResult<Unit> = gpsHandler.gpsOff()
        .withAppError(AppErrorCode.MEASUREMENT, "Pause GPS")

    private fun save(): AppResult<Unit> {
        val stream = outputStream
        val results = mutableListOf<AppResult<*>>()
        results += appResult(AppErrorCode.STORAGE, "Flush GPS measurement") { stream?.flush() }
        writeFailure?.let { results += AppResult.failure(it) }
        results += appResult(AppErrorCode.STORAGE, "Close GPS measurement") { stream?.close() }
        outputStream = null
        writeFailure = null
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Save GPS measurement")
    }

    suspend fun stop(): AppResult<Unit> = listOf(
        withContext(Dispatchers.Main) { pause() },
        withContext(Dispatchers.IO) { save() },
    ).combineAppResults(AppErrorCode.MEASUREMENT, "Stop GPS measurement")

    override fun onLocationChanged(location: Location?) {
        val sample = location ?: return
        appResult(AppErrorCode.STORAGE, "Write GPS sample") {
            outputStream?.write(locationRow(sample).toByteArray())
        }.onFailure { error ->
            if (writeFailure == null) {
                writeFailure = error
                mutableFailures.tryEmit(error)
            }
        }
    }

    private companion object {
        const val HEADER = "time_millis;latitude;longitude;altitude;accuracy;speed;bearing;provider\n"
    }
}
