package com.motionapps.sensorbox.fragments.displayers

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.NavArgs
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.SensorInfoView
import com.motionapps.sensorservices.handlers.GPSHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * Shows info about GPS to the InfoSensorFragment
 *
 * @property gpsHandler - manages access to GPS
 * @constructor
 * @param context
 */
class GPSDisplayer(context: Context, private val gpsHandler: GPSHandler) : Displayer, GPSHandler.OnLocationChangedCallback,
    OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var mapView: MapView? = null

    // attributes to show about GPS
    private val attrs: Array<SensorInfoView> = arrayOf(
        SensorInfoView(context.getString(R.string.sensor_info_GPS_latitude), "0", R.drawable.ic_latitude),
        SensorInfoView(context.getString(R.string.sensor_info_GPS_longitude), "0", R.drawable.ic_longitude),
        SensorInfoView(context.getString(R.string.sensor_info_GPS_altitude), "0", R.drawable.ic_altitude),
        SensorInfoView(context.getString(R.string.sensor_info_GPS_accuracy), "0", R.drawable.ic_accuracy),
        SensorInfoView(context.getString(R.string.sensor_info_GPS_speed),    "0", R.drawable.ic_speed),
        SensorInfoView(context.getString(R.string.sensor_info_GPS_bearing), "0", R.drawable.ic_bearing),
        SensorInfoView(context.getString(R.string.sensor_info_GPS_provider), "-", R.drawable.ic_provider)
    )

    // strings for mapping of the texts to update
    private val viewKeys: Array<String> = context.resources.getStringArray(R.array.sensor_info_GPS_array)
    private val floatFormatter: String = context.getString(R.string.sensor_info_float)
    // textViews to update
    private val valuesToUpdate: HashMap<String, TextView> = HashMap()

    override fun getView(context: Context, inflater: LayoutInflater, viewGroup: ViewGroup?, args: NavArgs): View {

        val view: View = inflater.inflate(R.layout.fragment_info_map, viewGroup, false)

        // set up map
        mapView = view.findViewById(R.id.mapView)
        this.mapView?.getMapAsync(this)
        this.mapView?.onCreate(null)
        this.mapView?.onStart()
        this.mapView?.onResume()

        // inflate attributes
        val linearLayout: LinearLayout = view.findViewById(R.id.sensorinfo_container)
        for(attr: SensorInfoView in attrs){
            val row: View = inflater.inflate(R.layout.item_layout_sensorrow_info, null)

            (row.findViewById<ImageView>(R.id.sensorrow_icon)).also { it.setImageResource(attr.icon) }
            (row.findViewById<TextView>(R.id.sensorrow_info_title)).also { it.text = attr.title }
            (row.findViewById<TextView>(R.id.sensorrow_info_value)).also {
                it.text = attr.value
                valuesToUpdate[attr.title] = it // save variable to update
            }

            linearLayout.addView(row)
        }

        gpsHandler.addCallback(context, this) // call for GPS

        return view
    }

    /**
     * If the location changes -> update views
     *
     * @param location
     */
    private fun updateGPS(location: Location?) {
        if (location == null) {
            return
        }
        if (googleMap != null) {
            googleMap!!.moveCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(
                        location.latitude,
                        location.longitude
                    )
                )
            )
            googleMap!!.moveCamera(CameraUpdateFactory.zoomTo(10f))
        }

        for (key: String in valuesToUpdate.keys){
            when(key){
                viewKeys[0] -> { // latitude
                    valuesToUpdate[key]?.text  = location.latitude.toString()
                }
                viewKeys[1] -> { // longitude
                    valuesToUpdate[key]?.text  = location.longitude.toString()
                }
                viewKeys[2] -> { // altitude [m]
                    valuesToUpdate[key]?.text  = floatFormatter.format(location.altitude)
                }
                viewKeys[3] -> { // accuracy [m]
                    valuesToUpdate[key]?.text  = floatFormatter.format(location.accuracy)
                }
                viewKeys[4] -> { // speed [m/s]
                    valuesToUpdate[key]?.text  = floatFormatter.format(location.speed)
                }
                viewKeys[5] -> { // bearing [°]
                    valuesToUpdate[key]?.text  = floatFormatter.format(location.bearing)
                }
                viewKeys[6] -> { // provider
                    valuesToUpdate[key]?.text  = location.provider.toString()
                }
            }
        }
    }

    override fun onDestroy() {
        valuesToUpdate.clear()
        gpsHandler.gpsOff()
        this.mapView?.onPause()
        this.mapView?.onStop()
        this.mapView?.onDestroy()

        mapView = null
        googleMap = null

    }


    override fun onLocationChanged(location: Location?) {
        updateGPS(location)
    }

    override fun onLastLocationSuccess(location: Location?) {
        updateGPS(location)
    }

    override fun onAvailabilityChanged(locationAvailability: LocationAvailability?) {}

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        this.googleMap = googleMap
        googleMap?.isMyLocationEnabled = true
        googleMap?.uiSettings?.isZoomControlsEnabled = true
    }
}