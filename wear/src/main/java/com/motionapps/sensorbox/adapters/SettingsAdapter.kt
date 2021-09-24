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
import com.motionapps.sensorservices.services.MeasurementService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class SettingsAdapter(private val context: Context, private val prefKey: String, private val optionsText: Array<String>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    // private val samplingRatesStrings: Array<String> = context.resources.getStringArray(R.array.sensor_delays)
    private var position: Int
    private var pickedView: View? = null // saves view to save

    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        // picked preferences
        position = PREFERENCES[prefKey]!!.indexOf(sharedPreferences.getInt(prefKey, 1))
    }

    /**
     * inflates views for sampling rates
     *
     * @param parent
     * @param viewType
     * @return
     */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = mInflater.inflate(R.layout.settings_sampling_row, parent, false)
        return ViewHolder(view, 0)
    }

    /**
     * turns on the sampling rate, which is chosen - stored in sharedPreferences
     *
     * @param holder for view - text is changed with button colour
     * @param position
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val textView = holder.itemView.findViewById<TextView>(R.id.settings_sampling_text)
        textView.text = optionsText[position]
        val imageView = holder.itemView.findViewById<ImageView>(R.id.settings_sampling_image)
        (holder as ViewHolder).speed = position
        if (position == this.position) {
            imageView.setImageResource(R.drawable.accept_deny_dialog_positive_bg)
            pickedView = holder.itemView
        } else {
            imageView.setImageResource(R.drawable.accept_deny_dialog_negative_bg)
        }
    }

    override fun getItemCount(): Int {
        return optionsText.size
    }

    /**
     * Creates clickListener to change sampling rate for the sensor - view is replaced by new and
     * stored to SharedPreferences
     *
     * @param itemView
     */
    private inner class ViewHolder(itemView: View, var speed: Int = 0) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener{view ->
                pickedView?.let{
                    val previousView = it.findViewById<ImageView>(R.id.settings_sampling_image)
                    previousView.setImageResource(R.drawable.accept_deny_dialog_negative_bg)
                }

                this@SettingsAdapter.position = speed
                val imageView = view.findViewById<ImageView>(R.id.settings_sampling_image)
                imageView.setImageResource(R.drawable.accept_deny_dialog_positive_bg)

                pickedView = view
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                val editor = sharedPreferences.edit()
                editor.putInt(prefKey, PREFERENCES[prefKey]!![speed])
                editor.apply()

                notifyItemChanged(speed)

            }
        }
    }

    companion object {
        const val SAMPLING_PREFERENCE = "SAMPLING_PREFERENCE"
        val PREFERENCES = hashMapOf(
            SAMPLING_PREFERENCE to arrayOf(SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_DELAY_UI, SensorManager.SENSOR_DELAY_NORMAL),
            MeasurementService.GPS_TIME to arrayOf(1, 30, 60, 120, 300, 600, 1800, 3600),
            MeasurementService.GPS_DISTANCE to arrayOf(1, 10, 50, 100, 250, 500, 1000),
        )
    }


}