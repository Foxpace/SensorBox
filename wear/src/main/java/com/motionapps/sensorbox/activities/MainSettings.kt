package com.motionapps.sensorbox.activities

import android.content.Intent
import android.content.SharedPreferences
import android.hardware.SensorManager
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.SettingsAdapter

/**
 * parameters for speed of the sensors, and if to stop the measurement if the battery is low
 *
 */
class MainSettings : WearableActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_settings)

        // click listeners
        var view = findViewById<View>(R.id.main_settings_battery_button)
        view.setOnClickListener(this)
        view = findViewById(R.id.main_settings_sampling_button)
        view.setOnClickListener(this)
    }

    /**
     * picks string for speed sensor
     *
     * @param sharedPreferences - to obtain settings
     */
    private fun updateSampling(sharedPreferences: SharedPreferences) {
        val textView = findViewById<TextView>(R.id.main_settings_sampling_value)
        when (sharedPreferences.getInt(SettingsAdapter.SAMPLING_PREFERENCE, 0)) {
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
            R.id.main_settings_sampling_button -> startActivity(
                Intent(
                    this,
                    SettingsSampling::class.java
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // check on settings
        updateWearBattery(sharedPreferences)
        updateSampling(sharedPreferences)
    }

    companion object {
        const val BATTERY_RESTRICTION = "BATTERY_RESTRICTION_WEAR"
    }
}