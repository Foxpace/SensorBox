package com.motionapps.sensorbox.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.wear.ambient.AmbientModeSupport
import androidx.wear.widget.BoxInsetLayout
import com.motionapps.sensorbox.communication.MsgListener
import com.motionapps.sensorbox.R
import com.motionapps.sensorservices.services.MeasurementService
import com.motionapps.wearoslib.WearOsConstants
import com.motionapps.wearoslib.WearOsHandler
import com.motionapps.wearoslib.WearOsListener
import com.motionapps.wearoslib.WearOsStates
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * launched, when the measurementService is active
 */
class StopActivity : AppCompatActivity(), WearOsListener, AmbientModeSupport.AmbientCallbackProvider {

    private val wearOsHandler: WearOsHandler = WearOsHandler()
    private var wearOsState: WearOsStates.PresenceResult? = null
    private var job: Job? = null

    private lateinit var ambientController: AmbientModeSupport.AmbientController

    private var running = false
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == MeasurementService.STOP_ACTIVITY) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stop_activity)

        // whole screen is button
        val view = findViewById<View>(R.id.stopButton)

        view.setOnClickListener{
            // stops the MeasurementActivity
            var intent = Intent(MeasurementService.STOP_SERVICE)
            sendBroadcast(intent)
            Toasty.info(this, R.string.go_to_phone_to_sync, Toasty.LENGTH_LONG).show()
            intent = Intent(this, MainActivity::class.java)

            job?.cancel()
            wearOsState?.let {
                if(it.present){
                    MsgListener.sendStatusStartingService(this, wearOsHandler, false)
                }
            }

            finish()
            startActivity(intent)
        }

        // changes colour to black, when inactive - as watch
        ambientController = AmbientModeSupport.attach(this)

        registerReceiver(broadcastReceiver, IntentFilter(MeasurementService.STOP_ACTIVITY))
        running = true

        // searches for phone
        job = CoroutineScope(Dispatchers.Main).launch {
            wearOsHandler.searchForWearOs(this@StopActivity, this@StopActivity, WearOsConstants.PHONE_APP_CAPABILITY)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (running) {
            unregisterReceiver(broadcastReceiver)
        }
        running = false

        job?.cancel()
        wearOsHandler.onDestroy()
    }

    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        if(wearOsStates is WearOsStates.PresenceResult){
            this.wearOsState = wearOsStates
        }
    }

    private inner class StopAmbientCallback : AmbientModeSupport.AmbientCallback() {
        /**
         * changes colour of the button to black, when inactive
         * @param ambientDetails
         */
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            val view: BoxInsetLayout = this@StopActivity.findViewById(R.id.stopButton)
            view.backgroundTintList = ContextCompat.getColorStateList(
                this@StopActivity,
                R.color.black_list_color
            )
        }
        /**
         * turns it back to red, when active
         */
        override fun onExitAmbient() {
            val view: BoxInsetLayout = findViewById(R.id.stopButton)
            view.backgroundTintList = ContextCompat.getColorStateList(
                this@StopActivity,
                R.color.red_list_color
            )
        }

        override fun onUpdateAmbient() {
            // Update the content
        }
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return StopAmbientCallback()
    }
}