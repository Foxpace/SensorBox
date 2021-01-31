package com.motionapps.sensorbox.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.motionapps.sensorbox.charts.GraphViewer
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.SensorBasicAdapter
import com.motionapps.sensorbox.adapters.SensorBasicAdapter.ItemClickListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class PickSensorShow: AppCompatActivity(), ItemClickListener {
    private var adapter: SensorBasicAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick_sensor)

        // main view
        val view: WearableRecyclerView = findViewById(R.id.recycler_pick_view)
        view.layoutManager = WearableLinearLayoutManager(this)
        view.isEdgeItemsCenteringEnabled = true

        // checks sensors
        val smm = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensors = smm.getSensorList(Sensor.TYPE_ALL)

        if (!sensors.isNullOrEmpty()) {
            // adapter with sensors
            adapter = SensorBasicAdapter(this, sensors)
            adapter!!.setClickListener(this)
            view.adapter = adapter
        } else {
            Toast.makeText(this, R.string.nosensors, Toast.LENGTH_SHORT).show()
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }


    private var tempIntent: Intent? = null

    /**
     * starts activity with chart and showing sensor
     *
     * @param view - clicked view
     * @param position - position of the view
     */
    override fun onItemClick(view: View?, position: Int, sensorId: Int) {
        val intent: Intent
        if(sensorId == -1){
            intent = Intent(this, MapsActivity::class.java)
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this@PickSensorShow,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_RESULT_GPS)
                tempIntent = intent
                return
            }
        }else{
            intent = Intent(this, GraphViewer::class.java)
            intent.putExtra(GET_EXTRA_TYPE, adapter!!.getItem(position))
            intent.putExtra(GET_EXTRA_NAME, adapter!!.getString(position))

            if(sensorId == Sensor.TYPE_HEART_RATE){
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this@PickSensorShow,
                        arrayOf(Manifest.permission.BODY_SENSORS), PERMISSION_RESULT_BODY)
                    tempIntent = intent
                    return
                }
            }
        }

        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_RESULT_BODY || requestCode == PERMISSION_RESULT_GPS){
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startActivity(tempIntent)
            } else {
                when(requestCode){
                    PERMISSION_RESULT_GPS -> {
                        if(shouldShowRequestPermissionRationale(permissions[0])){
                            Toast.makeText(this, R.string.permission_rejected_gps, Toast.LENGTH_LONG).show()
                        }else{
                            finish()
                            startActivity(Intent(this, PermissionActivity::class.java))
                        }
                    }
                    PERMISSION_RESULT_BODY -> {
                        if(shouldShowRequestPermissionRationale(permissions[0])){
                            Toast.makeText(this, R.string.permission_rejected_body, Toast.LENGTH_LONG).show()
                        }else{
                            finish()
                            startActivity(Intent(this, PermissionActivity::class.java))
                        }

                    }
                }

            }
        }
    }

    companion object {
        const val GET_EXTRA_TYPE = "SensorType"
        const val GET_EXTRA_NAME = "SensorName"
        const val PERMISSION_RESULT_BODY = 123
        const val PERMISSION_RESULT_GPS = 124
    }
}