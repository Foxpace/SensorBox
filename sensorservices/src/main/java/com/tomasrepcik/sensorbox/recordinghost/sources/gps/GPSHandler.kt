package com.tomasrepcik.sensorbox.recordinghost.sources.gps

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.core.failure.flatMap

@SuppressLint("MissingPermission")
class GPSHandler(private val locationClient: FusedLocationProviderClient? = null) : LocationCallback() {

    private var callback: OnLocationChangedCallback? = null
    private var lastLocation: Location? = null

    private fun initialize(client: FusedLocationProviderClient, intervalSeconds: Int, minDistanceMeters: Int) {
        client.lastLocation.addOnSuccessListener { location: Location? ->
            lastLocation = location
            callback?.onLastLocationSuccess(location)
        }.addOnFailureListener { error ->
            Log.e(TAG, "Cannot read last GPS location", error)
            callback?.onLastLocationSuccess(null)
        }

        val request = createRequest(intervalSeconds, minDistanceMeters)
        client.requestLocationUpdates(request, this, Looper.getMainLooper()).addOnFailureListener { error ->
            Log.e(TAG, "Cannot request GPS updates", error)
        }
    }

    override fun onLocationResult(locationResult: LocationResult) {
        val location = locationResult.lastLocation ?: return
        lastLocation = location
        callback?.onLocationChanged(location)
    }

    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
        callback?.onAvailabilityChanged(locationAvailability)
    }

    fun gpsOff(): AppResult<Unit> = appResult(AppErrorCode.RECORDING, "Stop GPS updates") {
        if (callback != null) {
            locationClient?.flushLocations()?.addOnFailureListener { error ->
                Log.e(TAG, "Cannot flush GPS updates", error)
            }
            locationClient?.removeLocationUpdates(this)?.addOnFailureListener { error ->
                Log.e(TAG, "Cannot remove GPS updates", error)
            }
        }
        callback = null
    }

    private fun createRequest(intervalSeconds: Int, minDistanceMeters: Int): LocationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        intervalSeconds.coerceIn(1, MAX_INTERVAL_SECONDS) * 1_000L,
    )
        .setMinUpdateDistanceMeters(minDistanceMeters.coerceIn(0, MAX_DISTANCE_METERS).toFloat())
        .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        .setWaitForAccurateLocation(true)
        .build()

    fun addCallback(
        gpsCallback: OnLocationChangedCallback,
        intervalSeconds: Int = DEFAULT_INTERVAL_SECONDS,
        minDistanceMeters: Int = DEFAULT_DISTANCE_METERS,
    ): AppResult<Unit> = (if (callback != null) gpsOff() else AppResult.success(Unit)).flatMap {
        appResult(AppErrorCode.RECORDING, "Register GPS callback") {
            val client = checkNotNull(locationClient) { "GPS client is unavailable" }
            callback = gpsCallback
            initialize(client, intervalSeconds, minDistanceMeters)
            gpsCallback.onLocationChanged(lastLocation)
        }
    }

    interface OnLocationChangedCallback {
        fun onLocationChanged(location: Location?)
        fun onLastLocationSuccess(location: Location?) = Unit
        fun onAvailabilityChanged(locationAvailability: LocationAvailability?) = Unit
    }

    private companion object {
        const val DEFAULT_INTERVAL_SECONDS = 10
        const val DEFAULT_DISTANCE_METERS = 20
        const val MAX_INTERVAL_SECONDS = 3_600
        const val MAX_DISTANCE_METERS = 10_000
        const val TAG = "SensorBox GPS"
    }
}
