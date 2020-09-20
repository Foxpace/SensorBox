package com.motionapps.sensorbox.activities

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.widget.Toast
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
class PickSensorShow : WearableActivity(), ItemClickListener {
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

    /**
     * starts activity with chart and showing sensor
     *
     * @param view - clicked view
     * @param position - position of the view
     */
    override fun onItemClick(view: View?, position: Int) {
        val intent = Intent(this, GraphViewer::class.java)
        intent.putExtra(GET_EXTRA_TYPE, adapter!!.getItem(position))
        intent.putExtra(GET_EXTRA_NAME, adapter!!.getString(position))
        startActivity(intent)
    }

    companion object {
        const val GET_EXTRA_TYPE = "SensorType"
        const val GET_EXTRA_NAME = "SensorName"
    }
}