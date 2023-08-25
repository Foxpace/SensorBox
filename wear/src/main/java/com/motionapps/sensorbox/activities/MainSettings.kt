package com.motionapps.sensorbox.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.preference.PreferenceManager
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.SettingsAdapter
import com.motionapps.sensorbox.adapters.SettingsPickerAdapter
import com.motionapps.sensorservices.services.MeasurementService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * parameters for speed of the sensors, and if to stop the measurement if the battery is low
 *
 */
@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MainSettings : ComponentActivity(), SettingsAdapter.ClickListenerInterface {

    private var settingsAdapter: SettingsAdapter? = null
    private var lastChange: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val recyclerView: WearableRecyclerView = findViewById(R.id.settings_recycler)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)
        recyclerView.isEdgeItemsCenteringEnabled = true

        settingsAdapter = SettingsAdapter(this)
        settingsAdapter?.clickListener = this
        recyclerView.adapter = settingsAdapter
    }

    override fun onClick(position: Int, key: String) {
        lastChange = position
        when (key) {
            // negate the preference
            BATTERY_RESTRICTION, WAKE_LOCK, ALWAYS_ON_DISPLAY  -> {
                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                val b = !sharedPreferences.getBoolean(key, SettingsAdapter.defaultValues.getOrDefault(key, false))
                val editor = sharedPreferences.edit()
                editor.putBoolean(key, b)
                editor.apply()

                settingsAdapter?.notifyItemChanged(position)

            }

            // needs new activity
            SettingsPickerAdapter.SAMPLING_PREFERENCE -> {
                val intent = Intent(this, SettingsPicker::class.java)
                intent.putExtra(
                    SettingsPicker.KEY_SETTINGS,
                    SettingsPickerAdapter.SAMPLING_PREFERENCE
                )
                startActivity(intent)
            }

            MeasurementService.GPS_TIME -> {
                val intent = Intent(this, SettingsPicker::class.java)
                intent.putExtra(SettingsPicker.KEY_SETTINGS, MeasurementService.GPS_TIME)
                startActivity(intent)
            }

            MeasurementService.GPS_DISTANCE -> {
                val intent = Intent(this, SettingsPicker::class.java)
                intent.putExtra(SettingsPicker.KEY_SETTINGS, MeasurementService.GPS_DISTANCE)
                startActivity(intent)
            }

        }
    }

    override fun onResume() {
        super.onResume()
        if(lastChange != -1){
            settingsAdapter?.notifyItemChanged(lastChange)
        }
    }

    companion object {
        const val BATTERY_RESTRICTION = "BATTERY_RESTRICTION_WEAR"
        const val WAKE_LOCK = "WAKE_LOCK_WEAR"
        const val ALWAYS_ON_DISPLAY = "ALWAYS_ON_DISPLAY"
    }
}