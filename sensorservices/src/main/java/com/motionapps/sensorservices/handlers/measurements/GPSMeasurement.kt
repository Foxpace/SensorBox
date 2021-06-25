package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.location.Location
import android.os.Bundle
import com.google.android.gms.location.LocationAvailability
import com.motionapps.sensorservices.handlers.GPSHandler
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.FOLDER_NAME
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.INTERNAL_STORAGE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.io.OutputStream

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * implements GPS handler to gather GPS data and write them into CSV
 *
 * @property gpsHandler - manages access to GPS
 */
class GPSMeasurement constructor(private val gpsHandler: GPSHandler): MeasurementInterface, GPSHandler.OnLocationChangedCallback {

    private var outputStream: OutputStream? = null
    private val header: String = "time_millis;latitude;longitude;altitude;accuracy;speed;bearing;provider\n"

    /**
     * creates outputStream based on the internal storage requirement
     *
     * @param context
     * @param params - from the service
     */
    override fun initMeasurement(context: Context, params: Bundle) {
        outputStream = if (params.getBoolean(INTERNAL_STORAGE)) {
            StorageHandler.createFileInInternalFolder(
                context,
                params.getString(FOLDER_NAME)!!,
                "GPS.csv"
            )

        } else {
            StorageHandler.createFileInFolder(
                context,
                params.getString(FOLDER_NAME)!!,
                "csv",
                "GPS.csv"
            )
        }
        // header
        outputStream?.apply{
            write(header.toByteArray())
        }
    }

    /**
     * parses attributes of the Location property
     *
     * @param location - location from the GPS
     * @return - formatted line of the csv
     */
    private fun createLocationStamp(location: Location): String {
        return "${System.currentTimeMillis()};" +
                "${location.latitude};" +
                "${location.longitude};" +
                "${location.altitude};" +
                "${location.accuracy};" +
                "${location.speed};" +
                "${location.bearing};" +
                location.provider + "\n"
    }

    /**
     * adds callback for the GPS
     *
     * @param context
     */

    override fun startMeasurement(context: Context) {
        gpsHandler.addCallback(context, this)
    }

    /**
     * turns off the GPS
     *
     * @param context
     */
    override fun pauseMeasurement(context: Context) {
        gpsHandler.gpsOff()
    }

    /**
     * outputStream is saved and closed
     *
     * @param context
     */
    override fun saveMeasurement(context: Context) {
        outputStream?.flush()
        outputStream?.close()
    }

    /**
     * save of the csv file and GPS is turned off
     *
     * @param context
     */
    override fun onDestroyMeasurement(context: Context) {
        pauseMeasurement(context)
        saveMeasurement(context)
    }

    /**
     * called on GPS change
     *
     * @param location - latest location
     */
    override fun onLocationChanged(location: Location?) {
        location?.let {loc: Location ->
            outputStream?.apply {
                write(createLocationStamp(loc).toByteArray())
            }
        }
    }

    override fun onLastLocationSuccess(location: Location?) {}

    override fun onAvailabilityChanged(locationAvailability: LocationAvailability?) {}
}