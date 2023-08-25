package com.motionapps.sensorbox.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.SettingsPickerAdapter
import com.motionapps.sensorbox.adapters.SettingsPickerAdapter.Companion.SAMPLING_PREFERENCE
import com.motionapps.sensorservices.services.MeasurementService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

/**
 * shows sampling rates for sensors - user can choose
 *
 */
@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class SettingsPicker: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings_picker)

        val recyclerView: WearableRecyclerView = findViewById(R.id.settings_recycler_pick_view)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)
        recyclerView.isEdgeItemsCenteringEnabled = true

        val texts = when (intent.getStringExtra(KEY_SETTINGS)) {
            SAMPLING_PREFERENCE -> {
                R.array.sensor_delays
            }
            MeasurementService.GPS_TIME -> {
                R.array.GPS_times
            }
            MeasurementService.GPS_DISTANCE -> {
                R.array.GPS_distances

            }
            else -> R.array.sensor_delays
        }

        val mainActivityAdapter = SettingsPickerAdapter(this,
            intent.getStringExtra(KEY_SETTINGS)!!,
            resources.getStringArray(texts))

        recyclerView.adapter = mainActivityAdapter
    }

    companion object{
        const val KEY_SETTINGS = "KEY_SETTINGS"
    }
}