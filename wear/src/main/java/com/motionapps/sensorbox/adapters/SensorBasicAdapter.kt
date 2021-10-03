package com.motionapps.sensorbox.adapters

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.motionapps.sensorbox.R
import java.util.*

/**
 * Shows view filled with available sensors
 *
 * @param context
 * @param data - list of available sensors
 */
class SensorBasicAdapter(context: Context?, data: List<Sensor>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mClickListener: ItemClickListener? = null
    private val availableSensors: MutableList<Int> = ArrayList()
    private val availableSensorsString: MutableList<Int> = ArrayList()

    // data is passed into the constructor
    init {
        for (i in data.indices) {
            if (sensorTypes.contains(data[i].type)) {
                availableSensors.add(data[i].type)
                availableSensorsString.add(pickString(data[i].type, sensorTypes, sensorTypesString))
            }
        }
        // check if GPS is specified
        context?.let {
            addGPS(it)
        }
    }

    private fun addGPS(context: Context){
        // check if GPS is specified
        if (context.packageManager?.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) == true) {
            availableSensors.add(-1) // adding it as sensor with negative number
            availableSensorsString.add(R.string.gps)
        }
    }

    /**
     * inflates rows for the sensor
     *
     * @param parent
     * @param viewType
     * @return
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = mInflater.inflate(R.layout.sensor_text_row, parent, false)
        return ViewHolder(view)
    }

    /**
     * adds texts to the textView
     *
     * @param holder - sensor text row to fill with text
     * @param position
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val textView = holder.itemView.findViewById<TextView>(R.id.sensorText)
        textView.setText(availableSensorsString[position])
    }

    override fun getItemCount(): Int {
        return availableSensors.size
    }

    // stores and recycles views as they are scrolled off screen
    private inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        override fun onClick(view: View) {
            if (mClickListener != null) mClickListener!!.onItemClick(view, bindingAdapterPosition, availableSensors[bindingAdapterPosition])
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    // convenience method for getting data at click position
    fun getItem(id: Int): Int {
        return availableSensors[id]
    }

    fun getString(id: Int): Int {
        return availableSensorsString[id]
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        mClickListener = itemClickListener
    }

    // parent activity will implement this method to respond to click events
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int, sensorId: Int)
    }

    companion object {
        var sensorTypes: List<Int> = object : ArrayList<Int>() {
            init {
                add(Sensor.TYPE_ACCELEROMETER)
                add(Sensor.TYPE_LINEAR_ACCELERATION)
                add(Sensor.TYPE_GYROSCOPE)
                add(Sensor.TYPE_MAGNETIC_FIELD)
                add(Sensor.TYPE_HEART_RATE)
                add(-1)
            }
        }
        var sensorTypesString: List<Int> = object : ArrayList<Int>() {
            init {
                add(R.string.sensor_gravity)
                add(R.string.sensor_linear_acceleration)
                add(R.string.sensor_gyroscope)
                add(R.string.sensor_magnet)
                add(R.string.heart_rate)
                add(R.string.gps)
            }
        }

        fun pickString(picked: Int, types: List<Int>, stringsId: List<Int>): Int {
            for (i in types.indices) {
                if (types[i] == picked) {
                    return stringsId[i]
                }
            }
            return 0
        }
    }


}