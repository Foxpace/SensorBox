package com.motionapps.sensorbox.adapters

import android.content.Context
import android.hardware.SensorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.motionapps.sensorbox.R

class SettingsAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val samplingRatesStrings: Array<String> = context.resources.getStringArray(R.array.sensor_delays)
    private val samplingPosition: Int
    private var pickedView: View? = null // saves view to save

    /**
     * inflates views for sampling rates
     *
     * @param parent
     * @param viewType
     * @return
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = mInflater.inflate(R.layout.settings_sampling_row, parent, false)
        return ViewHolder(view)
    }

    /**
     * turns on the sampling rate, which is chosen - stored in sharedPreferences
     *
     * @param holder for view - text is changed with button colour
     * @param position
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val textView = holder.itemView.findViewById<TextView>(R.id.settings_sampling_text)
        textView.text = samplingRatesStrings[position]
        val imageView = holder.itemView.findViewById<ImageView>(R.id.settings_sampling_image)

        if (position == samplingPosition) {
            imageView.setImageResource(R.drawable.accept_deny_dialog_positive_bg)
            pickedView = holder.itemView
        } else {
            imageView.setImageResource(R.drawable.accept_deny_dialog_negative_bg)
        }
    }

    override fun getItemCount(): Int {
        return samplingRatesStrings.size
    }

    /**
     * Creates clickListener to change sampling rate for the sensor - view is replaced by new and
     * stored to SharedPreferences
     *
     * @param itemView
     */
    private inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener{view ->
                var imageView = pickedView!!.findViewById<ImageView>(R.id.settings_sampling_image)
                imageView.setImageResource(R.drawable.accept_deny_dialog_negative_bg)

                imageView = view.findViewById(R.id.settings_sampling_image)
                imageView.setImageResource(R.drawable.accept_deny_dialog_positive_bg)

                pickedView = view
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.putInt(SAMPLING_PREFERENCE, adapterPosition)
                editor.apply()
            }
        }
    }

    companion object {
        const val SAMPLING_PREFERENCE = "SAMPLING_PREFERENCE"
    }

    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        // picked preferences
        samplingPosition = sharedPreferences.getInt(SAMPLING_PREFERENCE, SensorManager.SENSOR_DELAY_FASTEST)
    }
}