package com.tomasrepcik.sensorbox.sensorservices.handlers.measurements

import android.location.Location
import com.google.android.gms.location.LocationAvailability
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.withAppError
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.sensorservices.handlers.GPSHandler
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * implements GPS handler to gather GPS data and write them into CSV
 *
 * @property gpsHandler - manages access to GPS
 */
internal class GPSMeasurement(
    private val gpsHandler: GPSHandler,
    private val storage: MeasurementStorage,
    private val clock: EpochClock,
) : GPSHandler.OnLocationChangedCallback {

    private var outputStream: OutputStream? = null
    private var writeFailure: AppError? = null
    private val header: String = "time_millis;latitude;longitude;altitude;accuracy;speed;bearing;provider\n"

    /**
     * creates outputStream based on the internal storage requirement
     *
     * @param context
     * @param params - from the service
     */
    fun prepare(
        folderName: String,
        useInternalStorage: Boolean,
        intervalSeconds: Int,
        minimumDistanceMeters: Int,
    ): AppResult<Unit> {
        gpsHandler.configure(
            intervalSeconds = intervalSeconds,
            minDistanceMeters = minimumDistanceMeters,
        )
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
            output.write(header.toByteArray())
        }
        return headerResult.withAppError(AppErrorCode.MEASUREMENT, "Initialize GPS measurement")
    }

    /**
     * parses attributes of the Location property
     *
     * @param location - location from the GPS
     * @return - formatted line of the csv
     */
    private fun createLocationStamp(location: Location): String = "${clock.nowMillis()};" +
        "${location.latitude};" +
        "${location.longitude};" +
        "${location.altitude};" +
        "${location.accuracy};" +
        "${location.speed};" +
        "${location.bearing};" +
        location.provider + "\n"

    /**
     * adds callback for the GPS
     *
     * @param context
     */

    fun start(): AppResult<Unit> = gpsHandler.addCallback(this)
        .withAppError(AppErrorCode.MEASUREMENT, "Start GPS")

    /**
     * turns off the GPS
     *
     * @param context
     */
    private fun pause(): AppResult<Unit> = gpsHandler.gpsOff()
        .withAppError(AppErrorCode.MEASUREMENT, "Pause GPS")

    /**
     * outputStream is saved and closed
     *
     * @param context
     */
    private suspend fun save(): AppResult<Unit> {
        val stream = outputStream
        val results = mutableListOf<AppResult<*>>()
        results += appResult(AppErrorCode.STORAGE, "Flush GPS measurement") { stream?.flush() }
        writeFailure?.let { results += AppResult.failure(it) }
        results += appResult(AppErrorCode.STORAGE, "Close GPS measurement") { stream?.close() }
        outputStream = null
        writeFailure = null
        return results.combineAppResults(AppErrorCode.MEASUREMENT, "Save GPS measurement")
    }

    /**
     * save of the csv file and GPS is turned off
     *
     * @param context
     */
    suspend fun stop(): AppResult<Unit> = listOf(
        withContext(Dispatchers.Main) { pause() },
        withContext(Dispatchers.IO) { save() },
    ).combineAppResults(AppErrorCode.MEASUREMENT, "Stop GPS measurement")

    /**
     * called on GPS change
     *
     * @param location - latest location
     */
    override fun onLocationChanged(location: Location?) {
        location?.let { loc: Location ->
            appResult(AppErrorCode.STORAGE, "Write GPS sample") {
                outputStream?.write(createLocationStamp(loc).toByteArray())
            }.onFailure { writeFailure = it }
        }
    }

    override fun onLastLocationSuccess(location: Location?) = Unit

    override fun onAvailabilityChanged(locationAvailability: LocationAvailability?) = Unit
}
