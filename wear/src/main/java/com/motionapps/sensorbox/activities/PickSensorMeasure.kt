package com.motionapps.sensorbox.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.support.wearable.activity.WearableActivity
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import com.motionapps.sensorbox.communication.MsgListener
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.adapters.SensorPickerAdapter
import com.motionapps.sensorbox.adapters.SensorPickerAdapter.ItemClickListener
import com.motionapps.sensorbox.adapters.SettingsAdapter
import com.motionapps.sensorservices.services.MeasurementService.Companion.getIntentWearOs
import com.motionapps.wearoslib.WearOsConstants
import com.motionapps.wearoslib.WearOsHandler
import com.motionapps.wearoslib.WearOsListener
import com.motionapps.wearoslib.WearOsStates
import kotlinx.coroutines.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
/**
 * User can pick sensors, which will be measured by MeasurementActivity
 *
 */
class PickSensorMeasure : WearableActivity(), ItemClickListener, WearOsListener {

    private var adapter: SensorPickerAdapter? = null

    // checks for low battery
    private var receiverRegistered = false
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (Objects.requireNonNull(intent.action)) {


                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    if (level >= 16) {
                        onLowBattery = false
                        return
                    }
                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                    if (!sharedPreferences.getBoolean(MainSettings.BATTERY_RESTRICTION, true)) {
                        return
                    }
                    onLowBattery = true
                }

                Intent.ACTION_BATTERY_LOW -> {
                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                    if (!sharedPreferences.getBoolean(MainSettings.BATTERY_RESTRICTION, true)) {
                        return
                    }
                    onLowBattery = true
                }
                Intent.ACTION_BATTERY_OKAY -> onLowBattery = false
            }
        }
    }

    private var onLowBattery = false

    private val wearOsHandler: WearOsHandler = WearOsHandler()
    private var wearOsState: WearOsStates.PresenceResult? = null
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_pick_sensor_measure)
        // sets up recycleView
        val wearableRecyclerView: WearableRecyclerView = findViewById(R.id.recycler_pick_view)
        wearableRecyclerView.layoutManager = WearableLinearLayoutManager(this)
        wearableRecyclerView.isEdgeItemsCenteringEnabled = true

        // checks sensors if available
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        if (sensors == null || sensors.isEmpty()) {
            Toast.makeText(this, R.string.nosensors, Toast.LENGTH_SHORT).show()
            finish()
            startActivity(Intent(this, MainActivity::class.java))
            return
        }

        // adds adapter
        adapter = SensorPickerAdapter(this, sensors)
        adapter!!.setClickListener(this)
        wearableRecyclerView.adapter = adapter

        // checks on battery level
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (sharedPreferences.getBoolean(MainSettings.BATTERY_RESTRICTION, true)) {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_LOW)
            intent.action = Intent.ACTION_BATTERY_CHANGED
            intent.action = Intent.ACTION_BATTERY_OKAY
            registerReceiver(broadcastReceiver, intentFilter)
            receiverRegistered = true
        }

        // searches for the phone
        job = CoroutineScope(Dispatchers.Main).launch {
                wearOsHandler.searchForWearOs(
                    this@PickSensorMeasure,
                    this@PickSensorMeasure,
                    WearOsConstants.PHONE_APP_CAPABILITY
                )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (receiverRegistered) {
            unregisterReceiver(broadcastReceiver)
        }

        job?.cancel()
        wearOsHandler.onDestroy()
    }

    override fun onItemClicked(view: View?, position: Int) {

        // start button
        if (position + 1 == adapter!!.itemCount && !onLowBattery) {
            startMeasurement()
            return
        }

        // low battery toast
        if (onLowBattery) {
            Toast.makeText(this@PickSensorMeasure, R.string.low_battery, Toast.LENGTH_SHORT).show()
            return
        }

        // checks sensor to measure
        val imageView = view?.findViewById<ImageView>(R.id.viewholder_image)
        if (adapter!!.isChecked(position)) {
            imageView?.setImageResource(R.drawable.ic_save_red)
        } else {
            imageView?.setImageResource(R.drawable.ic_save_green)
        }
    }


    private fun startMeasurement() {
        if (adapter!!.isReady) {

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

            // pathing, sensors, battery check
            val path = "WEAR_" + millisToTimeString(System.currentTimeMillis())
            val sensorSpeed = sharedPreferences.getInt(SettingsAdapter.SAMPLING_PREFERENCE, 0)
            val batteryRestriction =
                sharedPreferences.getBoolean(MainSettings.BATTERY_RESTRICTION, true)

            // intent creation for Wear Os
            var intent =
                getIntentWearOs(this, path, adapter!!.sensors, sensorSpeed, batteryRestriction)

            // if the phone is available - send status
            wearOsState?.let {
                if(it.present){
                    MsgListener.sendStatusStartingService(this, wearOsHandler, true)
                }
            }

            startService(intent)
            intent = Intent(this, StopActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, R.string.at_least_one, Toast.LENGTH_SHORT).show()
        }
    }

    override suspend fun onWearOsStates(wearOsStates: WearOsStates) {
        if(wearOsStates is WearOsStates.PresenceResult){
            this.wearOsState = wearOsStates
        }
    }

    /**
     * @param time - milliseconds from the epoch
     * @param format - "dd_MM_yyyy_HH_mm_ss" as default
     * @return formats milliseconds date string
     */
    private fun millisToTimeString(time: Long, format: String = "dd_MM_yyyy_HH_mm_ss"): String? {
        val date = Date(time)
        val formatter: DateFormat = SimpleDateFormat(format, Locale.getDefault())
        return formatter.format(date)
    }


}