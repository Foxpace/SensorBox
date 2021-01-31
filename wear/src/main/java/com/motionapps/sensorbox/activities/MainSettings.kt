package com.motionapps.sensorbox.activities

import android.content.Intent
import android.content.SharedPreferences
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.SettingsAdapter
import com.motionapps.sensorservices.services.MeasurementService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * parameters for speed of the sensors, and if to stop the measurement if the battery is low
 *
 */
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MainSettings: AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_settings)

        // click listeners
        var view = findViewById<View>(R.id.main_settings_battery_button)
        view.setOnClickListener(this)
        view = findViewById(R.id.main_settings_sampling_button)
        view.setOnClickListener(this)
        view = findViewById(R.id.main_settings_gps_time_button)
        view.setOnClickListener(this)
        view = findViewById(R.id.main_settings_gps_distance_button)
        view.setOnClickListener(this)
    }

    /**
     * picks string for speed sensor
     *
     * @param sharedPreferences - to obtain settings
     */

    private fun updateSampling(sharedPreferences: SharedPreferences) {
        val textView = findViewById<TextView>(R.id.main_settings_sampling_value)
        when (sharedPreferences.getInt(SettingsAdapter.SAMPLING_PREFERENCE, 1)) {
            SensorManager.SENSOR_DELAY_FASTEST -> textView.setText(R.string.settings_wear_sampling_fastest)
            SensorManager.SENSOR_DELAY_GAME -> textView.setText(R.string.settings_wear_sampling_game)
            SensorManager.SENSOR_DELAY_UI -> textView.setText(R.string.settings_wear_sampling_ui)
            SensorManager.SENSOR_DELAY_NORMAL -> textView.setText(R.string.settings_wear_sampling_normal)
        }
    }

    /**
     * picks string for battery restriction
     *
     * @param sharedPreferences
     */
    private fun updateWearBattery(sharedPreferences: SharedPreferences) {
        val b = sharedPreferences.getBoolean(BATTERY_RESTRICTION, true)
        val textView = findViewById<TextView>(R.id.main_settings_battery_value)
        if (b) {
            textView.setText(R.string.settings_battery_on)
        } else {
            textView.setText(R.string.settings_battery_off)
        }
    }

    private fun updateGPSTime(sharedPreferences: SharedPreferences){
        val textView = findViewById<TextView>(R.id.main_settings_gps_time_value)
        val position = SettingsAdapter.PREFERENCES[MeasurementService.GPS_TIME]!!.indexOf(sharedPreferences.getInt(MeasurementService.GPS_TIME, 1))
        textView.text = resources.getStringArray(R.array.GPS_times)[position]
    }

    private fun updateGPSDistance(sharedPreferences: SharedPreferences){
        val textView = findViewById<TextView>(R.id.main_settings_gps_distance_value)
        val position = SettingsAdapter.PREFERENCES[MeasurementService.GPS_DISTANCE]!!.indexOf(sharedPreferences.getInt(MeasurementService.GPS_DISTANCE, 1))
        textView.text = resources.getStringArray(R.array.GPS_distances)[position]
    }


    override fun onClick(view: View) {
        when (view.id) {

            R.id.main_settings_battery_button -> {
                // negate the preference
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                val b = !sharedPreferences.getBoolean(BATTERY_RESTRICTION, true)
                val editor = sharedPreferences.edit()
                editor.putBoolean(BATTERY_RESTRICTION, b)
                editor.apply()
                val textView = findViewById<TextView>(R.id.main_settings_battery_value)
                if (b) {
                    textView.setText(R.string.settings_battery_on)
                } else {
                    textView.setText(R.string.settings_battery_off)
                }
            }

            // needs new activity
            R.id.main_settings_sampling_button -> {
                val intent = Intent(this, SettingsPicker::class.java)
                intent.putExtra(SettingsPicker.KEY_SETTINGS, SettingsAdapter.SAMPLING_PREFERENCE)
                startActivity(intent)
            }

            R.id.main_settings_gps_time_button ->{
                val intent = Intent(this, SettingsPicker::class.java)
                intent.putExtra(SettingsPicker.KEY_SETTINGS, MeasurementService.GPS_TIME)
                startActivity(intent)
            }

            R.id.main_settings_gps_distance_button ->{
                val intent = Intent(this, SettingsPicker::class.java)
                intent.putExtra(SettingsPicker.KEY_SETTINGS, MeasurementService.GPS_DISTANCE)
                startActivity(intent)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // check on settings
        updateWearBattery(sharedPreferences)
        updateSampling(sharedPreferences)
        updateGPSTime(sharedPreferences)
        updateGPSDistance(sharedPreferences)
    }

    companion object {
        const val BATTERY_RESTRICTION = "BATTERY_RESTRICTION_WEAR"
    }
}