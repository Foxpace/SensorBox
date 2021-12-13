package com.motionapps.sensorbox.adapters

import android.content.Context
import android.content.SharedPreferences
import android.hardware.SensorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.activities.MainSettings
import com.motionapps.sensorservices.services.MeasurementService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.*

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * picker for the settings to change in the app
 *
 * @param context
 */
class SettingsAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // texts to inflate
    private val texts: ArrayList<Int> = object : ArrayList<Int>() {
        init {
            add(R.string.battery_limit)
            add(R.string.wake_lock)
            add(R.string.keep_screen_on)
            add(R.string.wear_sampling_text)
            add(R.string.activity_gps_time)
            add(R.string.activity_gps_distance)
        }
    }

    // icons to inflate
    private val images: ArrayList<Int> = object : ArrayList<Int>() {
        init {
            add(R.drawable.ic_battery_full)
            add(R.drawable.ic_cpu)
            add(R.drawable.ic_brightness)
            add(R.drawable.ic_sampling)
            add(R.drawable.ic_baseline_timer)
            add(R.drawable.ic_baseline_location)
        }
    }

    // classes to use
    private val keys: ArrayList<String> = object : ArrayList<String>() {
        init {
            add(MainSettings.BATTERY_RESTRICTION)
            add(MainSettings.WAKE_LOCK)
            add(MainSettings.ALWAYS_ON_DISPLAY)
            add(SettingsPickerAdapter.SAMPLING_PREFERENCE)
            add(MeasurementService.GPS_TIME)
            add(MeasurementService.GPS_DISTANCE)
        }
    }

    private val stringGpsTime: Array<String> = context.resources.getStringArray(R.array.GPS_times)
    private val stringGpsDistance: Array<String> =
        context.resources.getStringArray(R.array.GPS_distances)
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    var clickListener: ClickListenerInterface? = null

    /**
     * inflates empty view
     *
     * @param parent
     * @param viewType
     * @return ButtonHolder with specific activity
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = mInflater.inflate(R.layout.button_settings, parent, false)
        return ButtonHolder(view)
    }

    /**
     * adds action to the views
     *
     * @param holder ButtonHolder, where the icon and text is inflated
     * @param position of the view
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val imageView = holder.itemView.findViewById<ImageView>(R.id.settings_button_image)
        imageView.setImageResource(images[position])
        val textView = holder.itemView.findViewById<TextView>(R.id.settings_button_text)
        textView.setText(texts[position])

        updateValueText(holder.itemView.findViewById(R.id.settings_button_value), keys[position])
    }

    private fun updateValueText(textView: TextView, key: String) {
        when (key) {

            MainSettings.BATTERY_RESTRICTION, MainSettings.WAKE_LOCK, MainSettings.ALWAYS_ON_DISPLAY -> {
                if (sharedPreferences.getBoolean(key, defaultValues.getOrDefault(key, false))) {
                    textView.setText(R.string.settings_on)
                } else {
                    textView.setText(R.string.settings_off)
                }
            }

            SettingsPickerAdapter.SAMPLING_PREFERENCE -> {
                when (sharedPreferences.getInt(SettingsPickerAdapter.SAMPLING_PREFERENCE, 1)) {
                    SensorManager.SENSOR_DELAY_FASTEST -> textView.setText(R.string.settings_wear_sampling_fastest)
                    SensorManager.SENSOR_DELAY_GAME -> textView.setText(R.string.settings_wear_sampling_game)
                    SensorManager.SENSOR_DELAY_UI -> textView.setText(R.string.settings_wear_sampling_ui)
                    SensorManager.SENSOR_DELAY_NORMAL -> textView.setText(R.string.settings_wear_sampling_normal)
                }
            }

            MeasurementService.GPS_TIME -> {
                val position =
                    SettingsPickerAdapter.PREFERENCES[MeasurementService.GPS_TIME]!!.indexOf(
                        sharedPreferences.getInt(MeasurementService.GPS_TIME, 1)
                    )
                textView.text = stringGpsTime[position]
            }

            MeasurementService.GPS_DISTANCE -> {
                val position =
                    SettingsPickerAdapter.PREFERENCES[MeasurementService.GPS_DISTANCE]!!.indexOf(
                        sharedPreferences.getInt(MeasurementService.GPS_DISTANCE, 1)
                    )
                textView.text = stringGpsDistance[position]
            }
        }
    }

    override fun getItemCount(): Int {
        return keys.size
    }

    /**
     * holds reference to classes after click
     *
     * @param itemView
     */
    private inner class ButtonHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        override fun onClick(view: View) {
            if (clickListener != null) {
                clickListener!!.onClick(bindingAdapterPosition, keys[bindingAdapterPosition])
            }
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    /**
     * provides callback for the activity to handle clicks from the adapter and passes specific class
     *
     */
    interface ClickListenerInterface {
        fun onClick(position: Int, key: String)
    }

    companion object{
        val defaultValues: Map<String, Boolean> = mapOf(
            MainSettings.BATTERY_RESTRICTION to true,
            MainSettings.WAKE_LOCK to false,
            MainSettings.ALWAYS_ON_DISPLAY to false
        )
    }

}