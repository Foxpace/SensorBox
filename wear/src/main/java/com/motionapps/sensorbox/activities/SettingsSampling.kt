package com.motionapps.sensorbox.activities

import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.SettingsAdapter

/**
 * shows sampling rates for sensors - user can choose
 *
 */
class SettingsSampling : WearableActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val recyclerView: WearableRecyclerView = findViewById(R.id.settings_recycler_pick_view)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)
        recyclerView.isEdgeItemsCenteringEnabled = true

        val mainActivityAdapter = SettingsAdapter(this)
        recyclerView.adapter = mainActivityAdapter
    }
}