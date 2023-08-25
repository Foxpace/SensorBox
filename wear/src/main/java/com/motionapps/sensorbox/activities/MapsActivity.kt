package com.motionapps.sensorbox.activities

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.wear.ambient.AmbientLifecycleObserver
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.motionapps.sensorbox.R
import com.motionapps.sensorservices.handlers.GPSHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class MapsActivity : ComponentActivity(), OnMapReadyCallback, GPSHandler.OnLocationChangedCallback,
    AmbientLifecycleObserver.AmbientLifecycleCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var mapView: MapView
    private val gpsHandler = GPSHandler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        mapView = findViewById(R.id.mapView)
        mapView.getMapAsync(this)
        mapView.onCreate(savedInstanceState)

        val button = findViewById<ImageButton>(R.id.map_back)
        button.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        AmbientLifecycleObserver(this, this)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isZoomControlsEnabled = true

        gpsHandler.addCallback(this, this)
    }

    private fun updateGPS(location: Location?) {
        if (location == null) {
            return
        }
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLng(
                LatLng(
                    location.latitude,
                    location.longitude
                )
            )
        )
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(10f))
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        gpsHandler.gpsOff()
    }


    override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
        mapView.onEnterAmbient(null)
        gpsHandler.gpsOff()
    }

    override fun onExitAmbient() {
        mapView.onExitAmbient()
        gpsHandler.addCallback(this@MapsActivity, this@MapsActivity)
    }

    override fun onUpdateAmbient() {

    }


    override fun onLocationChanged(location: Location?) {
        updateGPS(location)
    }

    override fun onLastLocationSuccess(location: Location?) {
        updateGPS(location)
    }

    override fun onAvailabilityChanged(locationAvailability: LocationAvailability?) {
    }
}