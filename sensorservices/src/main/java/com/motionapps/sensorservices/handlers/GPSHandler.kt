package com.motionapps.sensorservices.handlers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.preference.PreferenceManager

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.motionapps.sensorservices.services.MeasurementService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@SuppressLint("MissingPermission")
class GPSHandler : LocationCallback() {

    private var callback: OnLocationChangedCallback? = null
    private lateinit var request: LocationRequest
    private lateinit var locationClient: FusedLocationProviderClient

    private var locationAvailability: LocationAvailability? = null
    private var lastLocation: Location? = null

    private var registered: Boolean = false
    private var firstInit: Boolean = false
    private val tag = "GPS_location"

    /**
     * creation of the request and locationClient
     *
     * @param context
     */
    private fun firstInit(context: Context){
        request = createRequest(context)
        locationClient = LocationServices.getFusedLocationProviderClient(context)
        firstInit = true
    }

    /**
     * calls for last known location and registers location callback
     *
     * @param context
     */
    private fun initialize(context: Context){

        if(!firstInit){
            firstInit(context)
        }

        locationClient.lastLocation.addOnSuccessListener { location ->
            lastLocation = location
            callback?.onLastLocationSuccess(location)
        }.addOnFailureListener {
            callback!!.onLastLocationSuccess(null)
        }

        locationClient.requestLocationUpdates(request, this, Looper.getMainLooper())
        registered = true
    }

    /**
     * saves last location and is passed if the new callback registers
     *
     * @param locationResult
     */
    override fun onLocationResult(locationResult: LocationResult) {
        super.onLocationResult(locationResult)
        if (locationResult.locations.size > 0) {
            lastLocation = locationResult.lastLocation
            if (lastLocation != null) {
                callback?.onLocationChanged(lastLocation)
            }
        }
    }

    /**
     * changes if the location cahnges provider / GPS is off
     *
     * @param locationAvailability
     */
    override fun onLocationAvailability(locationAvailability: LocationAvailability) {
        super.onLocationAvailability(locationAvailability)
        this.locationAvailability = locationAvailability
        callback?.onAvailabilityChanged(locationAvailability)
    }

    /**
     * removes GPS - no updates will be passed
     *
     */
    fun gpsOff() {
        if(registered){
            Log.i(tag, "Logging off location")
            locationClient.removeLocationUpdates(this)
        }
        registered = false
    }

    /**
     * parameters for locationClient - received from sharedPreferences - user can change them in Settings
     *
     * @param context
     * @return LocationRequest specified by user
     */
    private fun createRequest(context: Context): LocationRequest {

        val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val locationRequest = LocationRequest.create().apply {
            try {
                this.interval = sharedPreferences.getString(MeasurementService.GPS_TIME, "10")!!.toLong() * 1000L
                this.fastestInterval = sharedPreferences.getString(MeasurementService.GPS_TIME, "10")!!.toLong() * 1000L
                this.smallestDisplacement = sharedPreferences.getString(MeasurementService.GPS_DISTANCE, "20")!!.toFloat()
                this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }catch (e: ClassCastException){
                this.interval = sharedPreferences.getInt(MeasurementService.GPS_TIME, 10) * 1000L
                this.fastestInterval = sharedPreferences.getInt(MeasurementService.GPS_TIME, 10) * 1000L
                this.smallestDisplacement = sharedPreferences.getInt(MeasurementService.GPS_DISTANCE, 20).toFloat()
                this.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        }
        //registering GPS
        Log.i("GPS", "location request created")
        return locationRequest
    }

    /**
     * adding callback to pass location
     *
     * @param context
     * @param gpsCallback - this object will get access to location and updates, previous is forgotten
     *
     */
    fun addCallback(context: Context, gpsCallback: OnLocationChangedCallback) {
        if(registered){
            gpsOff()
        }

        initialize(context)
        callback = gpsCallback
        gpsCallback.onLocationChanged(lastLocation)
    }

    interface OnLocationChangedCallback {
        fun onLocationChanged(location: Location?)
        fun onLastLocationSuccess(location: Location?)
        fun onAvailabilityChanged(locationAvailability: LocationAvailability?)
    }
}