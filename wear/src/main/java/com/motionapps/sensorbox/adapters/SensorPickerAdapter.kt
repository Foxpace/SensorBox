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


class SensorPickerAdapter(context: Context?, sensors: List<Sensor>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mClickListener: ItemClickListener? = null
    private var button: View? = null
    val availableSensors: MutableList<Int> = ArrayList()
    private val availableSensorsString: MutableList<Int> = ArrayList()
    private val toMeasure = ArrayList<Boolean>()
    var gpsExists = false

    init {
        for (i in sensors.indices) {
            if (SensorBasicAdapter.sensorTypes.contains(sensors[i].type)) {
                availableSensors.add(sensors[i].type)
                availableSensorsString.add(
                    SensorBasicAdapter.pickString(
                        sensors[i].type,
                        SensorBasicAdapter.sensorTypes,
                        SensorBasicAdapter.sensorTypesString
                    )
                )
                toMeasure.add(false)
            }
        }

        context?.let {
            addGPS(it)
        }

    }

    private fun addGPS(context: Context){
        // check if GPS is specified
        if (context.packageManager?.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS) == true) {
            availableSensors.add(-1) // adding it as sensor with negative number
            availableSensorsString.add(R.string.gps)
            toMeasure.add(false)
            gpsExists = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return if (viewType == R.layout.button_picksensor_start) {
            ButtonHolder(mInflater.inflate(R.layout.button_picksensor_start, parent, false))

        } else {
            ViewHolder(mInflater.inflate(R.layout.button_picksensor_row, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < availableSensors.size) {
            val textView = holder.itemView.findViewById<TextView>(R.id.sensorText)
            textView.setText(availableSensorsString[position])
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == availableSensors.size) R.layout.button_picksensor_start else R.layout.button_picksensor_row
    }

    override fun getItemCount(): Int {
        return availableSensors.size + 1
    }

    fun isChecked(position: Int): Boolean {
        val b = toMeasure[position]
        toMeasure[position] = !toMeasure[position]
        return b
    }

    val isReady: Boolean
        get() {
            for (b in toMeasure) {
                if (b) {
                    return true
                }
            }
            return false
        }

    val sensors: IntArray
        get() {
            val types = ArrayList<Int>()
            for (i in availableSensors.indices) {
                if (toMeasure[i] && availableSensors[i] != -1) {
                    types.add(availableSensors[i])
                }
            }
            val arr = IntArray(types.size)
            for (i in types.indices) arr[i] = types[i]
            return arr
        }

    val isGPS: Boolean
        get() {
            return gpsExists && toMeasure.last()
        }

    private inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        override fun onClick(view: View) {
            if (mClickListener != null) {
                mClickListener!!.onItemClicked(view, bindingAdapterPosition)
            }
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    private inner class ButtonHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        override fun onClick(view: View) {
            if (mClickListener != null) {
                mClickListener!!.onItemClicked(view, bindingAdapterPosition)
            }
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    fun setClickListener(mClickListener: ItemClickListener?) {
        this.mClickListener = mClickListener
        if (button != null) {
            button!!.setOnClickListener(mClickListener as View.OnClickListener?)
        }
    }

    interface ItemClickListener {
        fun onItemClicked(view: View?, position: Int)
    }

}