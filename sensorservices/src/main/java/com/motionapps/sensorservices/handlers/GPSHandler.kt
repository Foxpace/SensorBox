package com.motionapps.sensorservices.handlers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap

@SuppressLint("MissingPermission")
class GPSHandler : LocationCallback() {

    private var callback: OnLocationChangedCallback? = null
    private lateinit var request: LocationRequest
    private lateinit var locationClient: FusedLocationProviderClient

    private var locationAvailability: LocationAvailability? = null
    private var lastLocation: Location? = null

    private var registered: Boolean = false
    private var firstInit: Boolean = false
    private var intervalSeconds: Int = DEFAULT_INTERVAL_SECONDS
    private var minDistanceMeters: Int = DEFAULT_DISTANCE_METERS
    private val tag = "GPS_location"

    /**
     * creation of the request and locationClient
     *
     * @param context
     */
    private fun firstInit(context: Context) {
        request = createRequest()
        locationClient = LocationServices.getFusedLocationProviderClient(context)
        firstInit = true
    }

    /**
     * calls for last known location and registers location callback
     *
     * @param context
     */
    private fun initialize(context: Context) {
        if (!firstInit) {
            firstInit(context)
        }

        locationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location == null) {
                callback?.onLastLocationSuccess(null)
            } else {
                lastLocation = location
                callback?.onLastLocationSuccess(location)
            }
        }.addOnFailureListener { error ->
            AppError.from(AppErrorCode.MEASUREMENT, "Read last GPS location", error)
            callback?.onLastLocationSuccess(null)
        }

        locationClient.requestLocationUpdates(request, this, Looper.getMainLooper()).addOnFailureListener { error ->
            AppError.from(AppErrorCode.MEASUREMENT, "Request GPS updates", error)
        }
        registered = true
    }

    /**
     * saves last location and is passed if the new callback registers
     *
     * @param locationResult
     */
    override fun onLocationResult(locationResult: LocationResult) {
        super.onLocationResult(locationResult)
        if (locationResult.locations.isNotEmpty()) {
            lastLocation = locationResult.lastLocation
            if (lastLocation != null) {
                callback?.onLocationChanged(lastLocation)
            }
        }
    }

    /** Reports provider availability changes to the active measurement. */
    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
        super.onLocationAvailability(locationAvailability)
        this.locationAvailability = locationAvailability
        callback?.onAvailabilityChanged(locationAvailability)
    }

    /** Stops location updates for the active measurement. */
    fun gpsOff(): AppResult<Unit> = appResult(AppErrorCode.MEASUREMENT, "Stop GPS updates") {
        if (registered) {
            Log.i(tag, "Logging off location")
            locationClient.flushLocations().addOnFailureListener { error ->
                AppError.from(AppErrorCode.MEASUREMENT, "Flush GPS updates", error)
            }
            locationClient.removeLocationUpdates(this).addOnFailureListener { error ->
                AppError.from(AppErrorCode.MEASUREMENT, "Remove GPS updates", error)
            }
        }
        registered = false
    }

    /** Creates a request from the immutable measurement configuration. */
    private fun createRequest(): LocationRequest {
        val builder = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalSeconds * 1000L,
        )
        builder.setMinUpdateDistanceMeters(minDistanceMeters.toFloat())
        builder.setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        builder.setWaitForAccurateLocation(true)
        Log.i("GPS", "location request created")
        return builder.build()
    }

    fun configure(intervalSeconds: Int, minDistanceMeters: Int) {
        this.intervalSeconds = intervalSeconds.coerceIn(1, MAX_INTERVAL_SECONDS)
        this.minDistanceMeters = minDistanceMeters.coerceIn(0, MAX_DISTANCE_METERS)
    }

    /**
     * adding callback to pass location
     *
     * @param context
     * @param gpsCallback - this object will get access to location and updates, previous is forgotten
     *
     */
    fun addCallback(context: Context, gpsCallback: OnLocationChangedCallback): AppResult<Unit> =
        (if (registered) gpsOff() else AppResult.success(Unit)).flatMap {
            appResult(AppErrorCode.MEASUREMENT, "Register GPS callback") {
                callback = gpsCallback
                initialize(context)
                gpsCallback.onLocationChanged(lastLocation)
            }
        }

    interface OnLocationChangedCallback {
        fun onLocationChanged(location: Location?)
        fun onLastLocationSuccess(location: Location?)
        fun onAvailabilityChanged(locationAvailability: LocationAvailability?)
    }

    private companion object {
        const val DEFAULT_INTERVAL_SECONDS = 10
        const val DEFAULT_DISTANCE_METERS = 20
        const val MAX_INTERVAL_SECONDS = 3_600
        const val MAX_DISTANCE_METERS = 10_000
    }
}
