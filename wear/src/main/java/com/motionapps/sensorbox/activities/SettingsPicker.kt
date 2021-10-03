package com.motionapps.sensorbox.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.SettingsAdapter
import com.motionapps.sensorbox.adapters.SettingsAdapter.Companion.SAMPLING_PREFERENCE
import com.motionapps.sensorservices.services.MeasurementService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * shows sampling rates for sensors - user can choose
 *
 */
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class SettingsPicker: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val recyclerView: WearableRecyclerView = findViewById(R.id.settings_recycler_pick_view)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)
        recyclerView.isEdgeItemsCenteringEnabled = true

        val title = findViewById<TextView>(R.id.settings_picker_title)
        val texts = when (intent.getStringExtra(KEY_SETTINGS)) {
            SAMPLING_PREFERENCE -> {
                R.array.sensor_delays
            }
            MeasurementService.GPS_TIME -> {
                title.text = getString(R.string.gps_time_title)
                R.array.GPS_times
            }
            MeasurementService.GPS_DISTANCE -> {
                title.text = getString(R.string.gps_dist_title)
                R.array.GPS_distances

            }
            else -> R.array.sensor_delays
        }

        val mainActivityAdapter = SettingsAdapter(this,
            intent.getStringExtra(KEY_SETTINGS)!!,
            resources.getStringArray(texts))

        recyclerView.adapter = mainActivityAdapter
    }

    companion object{
        const val KEY_SETTINGS = "KEY_SETTINGS"
    }
}