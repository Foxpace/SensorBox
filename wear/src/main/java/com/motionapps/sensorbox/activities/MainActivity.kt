package com.motionapps.sensorbox.activities

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.wearable.activity.WearableActivity
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.MainActivityAdapter
import com.motionapps.sensorbox.adapters.MainActivityAdapter.ClickListenerInterface
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.sensorservices.services.MeasurementService.MeasurementBinder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * MainActivity for Wear os - let you choose from activities
 *
 */
class MainActivity : WearableActivity(), ClickListenerInterface {

    // checks if the service is alive - switches to active state
    private val connection: ServiceConnection = object  : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            if (iBinder is MeasurementBinder) {
                val measurementService = iBinder.getService()
                if (measurementService.running) {
                    finish()
                    startActivity(Intent(this@MainActivity, StopActivity::class.java))
                }
            }
            unbindService(this)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {}
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // adding recycleview and adapter to scroll through the activities
        val recyclerView: WearableRecyclerView = findViewById(R.id.menu_recycler_pick_view)
        recyclerView.layoutManager = WearableLinearLayoutManager(this)
        recyclerView.isEdgeItemsCenteringEnabled = true

        val mainActivityAdapter = MainActivityAdapter(this)
        mainActivityAdapter.clickListener = this
        recyclerView.adapter = mainActivityAdapter
    }

    override fun onResume() {
        super.onResume()

        bindService(
            Intent(this, MeasurementService::class.java),
            connection,
            BIND_AUTO_CREATE
        )
    }

    override fun onClick(c: Class<*>?) {
        startActivity(Intent(this, c))
    }
}