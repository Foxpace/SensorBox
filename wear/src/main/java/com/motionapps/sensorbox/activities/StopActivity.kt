package com.motionapps.sensorbox.activities

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
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
class StopActivity : AppCompatActivity(), WearOsListener,
    AmbientModeSupport.AmbientCallbackProvider {

    private val wearOsHandler: WearOsHandler = WearOsHandler()
    private var wearOsState: WearOsStates.PresenceResult? = null
    private var job: Job? = null

    private lateinit var ambientController: AmbientModeSupport.AmbientController

    private val handler: Handler = Handler(Looper.getMainLooper())

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

        view.setOnClickListener {
            // stops the MeasurementActivity
            var intent = Intent(MeasurementService.STOP_SERVICE)
            sendBroadcast(intent)
            Toasty.info(this, R.string.go_to_phone_to_sync, Toasty.LENGTH_LONG).show()
            intent = Intent(this, MainActivity::class.java)

            job?.cancel()
            wearOsState?.let {
                if (it.present) {
                    MsgListener.sendStatusStartingService(this, wearOsHandler, false)
                }
            }

            finish()
            startActivity(intent)
        }

        // check the always on display option + sets up the black screen after 1 min
        val sharedPreferences: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences.getBoolean(MainSettings.ALWAYS_ON_DISPLAY, false)) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            findViewById<TextView>(R.id.stop_activity_text)?.let {
                it.text = getString(R.string.tap_to_save_screen_on)
            }
            handler.postDelayed(Runnable {
                turnScreenBlack()
            }, 60_000)
        }


        // changes colour to black, when inactive - as watch
        ambientController = AmbientModeSupport.attach(this)

        // cancel from the phone by communication channel -> receiver
        registerReceiver(broadcastReceiver, IntentFilter(MeasurementService.STOP_ACTIVITY))
        running = true

        // searches for phone
        job = CoroutineScope(Dispatchers.Main).launch {
            wearOsHandler.searchForWearOs(
                this@StopActivity,
                this@StopActivity,
                WearOsConstants.PHONE_APP_CAPABILITY
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (running) {
            unregisterReceiver(broadcastReceiver)
        }
        running = false

        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.removeCallbacksAndMessages(null)

        job?.cancel()
        wearOsHandler.onDestroy()
    }

    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        if (wearOsStates is WearOsStates.PresenceResult) {
            this.wearOsState = wearOsStates
        }
    }

    private inner class StopAmbientCallback : AmbientModeSupport.AmbientCallback() {
        /**
         * changes colour of the button to black, when inactive
         * @param ambientDetails
         */
        override fun onEnterAmbient(ambientDetails: Bundle?) {
            turnScreenBlack()
        }

        /**
         * turns it back to red, when active
         */
        override fun onExitAmbient() {
            turnScreenRed()
        }

        override fun onUpdateAmbient() {
            // Update the content
        }
    }

    private fun turnScreenRed() {
        val view: BoxInsetLayout = findViewById(R.id.stopButton)
        view.backgroundTintList = ContextCompat.getColorStateList(
            this@StopActivity,
            R.color.red_list_color
        )
    }

    private fun turnScreenBlack() {
        val view: BoxInsetLayout = this@StopActivity.findViewById(R.id.stopButton)
        view.backgroundTintList = ContextCompat.getColorStateList(
            this@StopActivity,
            R.color.black_list_color
        )
    }

    override fun getAmbientCallback(): AmbientModeSupport.AmbientCallback {
        return StopAmbientCallback()
    }
}