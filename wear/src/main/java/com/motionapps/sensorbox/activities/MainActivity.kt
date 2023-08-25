package com.motionapps.sensorbox.activities

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.MainActivityAdapter
import com.motionapps.sensorbox.adapters.MainActivityAdapter.ClickListenerInterface
import com.motionapps.sensorbox.adapters.MainActivityAdapter.Companion.PHONE_INFO
import com.motionapps.sensorbox.adapters.MainActivityAdapter.Companion.PRIVACY_POLICY
import com.motionapps.sensorbox.adapters.MainActivityAdapter.Companion.SENSOR_MEASUREMENT
import com.motionapps.sensorbox.adapters.MainActivityAdapter.Companion.SENSOR_SHOW
import com.motionapps.sensorbox.adapters.MainActivityAdapter.Companion.SETTINGS
import com.motionapps.sensorbox.adapters.MainActivityAdapter.Companion.TERMS
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.sensorservices.services.MeasurementService.MeasurementBinder
import com.motionapps.wearoslib.WearOsHandler
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi


@ExperimentalCoroutinesApi
@InternalCoroutinesApi

/**
 * MainActivity for Wear os - let you choose from activities
 */
class MainActivity: ComponentActivity(), ClickListenerInterface {

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

    override fun onClick(action: Int) {
        when(action){
            SENSOR_MEASUREMENT -> startActivity(Intent(this, PickSensorMeasure::class.java))
            SENSOR_SHOW -> startActivity(Intent(this, PickSensorShow::class.java))
            PHONE_INFO -> startActivity(Intent(this, MoveToMain::class.java))
            SETTINGS -> startActivity( Intent(this, MainSettings::class.java))
            PRIVACY_POLICY -> startBrowser(this, R.string.link_privacy_policy)
            TERMS -> startBrowser(this, R.string.link_terms)
        }
    }



    companion object{
        fun startBrowser(context: Context, urlID: Int){

            val remoteActivityHelper = RemoteActivityHelper(context)

            remoteActivityHelper.startRemoteActivity(
                Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData( Uri.parse(context.getString(urlID))),
                WearOsHandler().getNodeId(context)
            )
            Toasty.info(context, R.string.open_phone_browser, Toasty.LENGTH_SHORT, true).show()
        }
    }
}