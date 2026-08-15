package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.location.Location
import android.os.Bundle
import com.google.android.gms.location.LocationAvailability
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.combineAppResults
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorservices.handlers.GPSHandler
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.FOLDER_NAME
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.INTERNAL_STORAGE
import com.motionapps.sensorservices.services.MeasurementService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

/**
 * implements GPS handler to gather GPS data and write them into CSV
 *
 * @property gpsHandler - manages access to GPS
 */
class GPSMeasurement constructor(private val gpsHandler: GPSHandler) :
    MeasurementInterface,
    GPSHandler.OnLocationChangedCallback {

    private var outputStream: OutputStream? = null
    private var writeFailure: AppError? = null
    private val header: String = "time_millis;latitude;longitude;altitude;accuracy;speed;bearing;provider\n"

    /**
     * creates outputStream based on the internal storage requirement
     *
     * @param context
     * @param params - from the service
     */
    override fun initMeasurement(context: Context, params: Bundle): Result<Unit> {
        gpsHandler.configure(
            intervalSeconds = params.getInt(MeasurementService.GPS_INTERVAL_SECONDS, 10),
            minDistanceMeters = params.getInt(MeasurementService.GPS_DISTANCE_METERS, 20),
        )
        val stream = if (params.getBoolean(INTERNAL_STORAGE)) {
            StorageHandler.createFileInInternalFolder(
                context,
                params.getString(FOLDER_NAME).orEmpty(),
                "gps.csv",
            )
        } else {
            StorageHandler.createFileInFolder(
                context,
                params.getString(FOLDER_NAME).orEmpty(),
                "csv",
                "gps.csv",
            )
        }
        return stream.flatMap { output ->
            outputStream = output
            appResult(AppError.Kind.STORAGE, "Write GPS header") { output.write(header.toByteArray()) }
        }.withAppError(AppError.Kind.MEASUREMENT, "Initialize GPS measurement")
    }

    /**
     * parses attributes of the Location property
     *
     * @param location - location from the GPS
     * @return - formatted line of the csv
     */
    private fun createLocationStamp(location: Location): String = "${System.currentTimeMillis()};" +
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

    override fun startMeasurement(context: Context): Result<Unit> = gpsHandler.addCallback(context, this)
        .withAppError(AppError.Kind.MEASUREMENT, "Start GPS")

    /**
     * turns off the GPS
     *
     * @param context
     */
    override fun pauseMeasurement(context: Context): Result<Unit> = gpsHandler.gpsOff()
        .withAppError(AppError.Kind.MEASUREMENT, "Pause GPS")

    /**
     * outputStream is saved and closed
     *
     * @param context
     */
    override suspend fun saveMeasurement(context: Context): Result<Unit> {
        val stream = outputStream
        val results = mutableListOf<Result<*>>()
        results += appResult(AppError.Kind.STORAGE, "Flush GPS measurement") { stream?.flush() }
        writeFailure?.let { results += Result.failure<Unit>(it) }
        results += appResult(AppError.Kind.STORAGE, "Close GPS measurement") { stream?.close() }
        outputStream = null
        writeFailure = null
        return results.combineAppResults(AppError.Kind.MEASUREMENT, "Save GPS measurement")
    }

    /**
     * save of the csv file and GPS is turned off
     *
     * @param context
     */
    override suspend fun onDestroyMeasurement(context: Context): Result<Unit> = listOf(
        withContext(Dispatchers.Main) { pauseMeasurement(context) },
        withContext(Dispatchers.IO) { saveMeasurement(context) },
    ).combineAppResults(AppError.Kind.MEASUREMENT, "Stop GPS measurement")

    /**
     * called on GPS change
     *
     * @param location - latest location
     */
    override fun onLocationChanged(location: Location?) {
        location?.let { loc: Location ->
            appResult(AppError.Kind.STORAGE, "Write GPS sample") {
                outputStream?.write(createLocationStamp(loc).toByteArray())
            }.onFailure { writeFailure = it as AppError }
        }
    }

    override fun onLastLocationSuccess(location: Location?) = Unit

    override fun onAvailabilityChanged(locationAvailability: LocationAvailability?) = Unit
}
