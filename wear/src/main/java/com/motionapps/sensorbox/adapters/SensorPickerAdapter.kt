package com.motionapps.sensorbox.adapters

import android.content.Context
import android.hardware.Sensor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.motionapps.sensorbox.R
import java.util.*

class SensorPickerAdapter(context: Context?, sensors: List<Sensor>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private var mClickListener: ItemClickListener? = null
    private var button: View? = null
    private val availableSensors: MutableList<Int> = ArrayList()
    private val availableSensorsString: MutableList<Int> = ArrayList()
    private val toMeasure = ArrayList<Boolean>()
    private var counter = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (++counter > availableSensors.size) {
            button = mInflater.inflate(R.layout.button_picksensor_start, parent, false)
            button?.let {
                return ButtonHolder(it)
            }
        }

        val view: View = mInflater.inflate(R.layout.button_picksensor_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder !is ButtonHolder) {
            val textView = holder.itemView.findViewById<TextView>(R.id.sensorText)
            textView.setText(availableSensorsString[position])
        }
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
                if (toMeasure[i]) {
                    types.add(availableSensors[i])
                }
            }
            val arr = IntArray(types.size)
            for (i in types.indices) arr[i] = types[i]
            return arr
        }

    private inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        override fun onClick(view: View) {
            if (mClickListener != null) {
                mClickListener!!.onItemClicked(view, adapterPosition)
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
                mClickListener!!.onItemClicked(view, adapterPosition)
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
    }
}