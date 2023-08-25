package com.motionapps.sensorbox.rlRecording

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.motionapps.wearoslib.WearOsConstants.SAMPLE_PATH
import com.motionapps.wearoslib.WearOsConstants.SAMPLE_PATH_TIME
import com.motionapps.wearoslib.WearOsConstants.SAMPLE_PATH_VALUE
import com.motionapps.wearoslib.WearOsConstants.WEAR_END_SENSOR_REAL_TIME


/**
 * Service to send data from Wear Os to phone
 *
 */
class RealTimeSensorService : Service(), SensorEventListener {

    private var mDataClient: DataClient? = null
    private var running = false

    // intent to stop the service
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != null) {
                if (WEAR_END_SENSOR_REAL_TIME == intent.action) {
                    stopSelf()
                }
            }
        }
    }

    /**
     * registers the dataClient, broadcastReceiver, which can stop service, registers sensor, which samples are sent to Phone
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (running) {
            unregisterSensor()
        }

        val b = intent.extras
        if (b != null) {
            val id = b.getInt(SENSOR_ID, -1)
            if (id != -1) {
                mDataClient = Wearable.getDataClient(this)
                registerBroadcast()
                registerSensor(id)
            } else {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBroadcast() {
        val filter = IntentFilter(WEAR_END_SENSOR_REAL_TIME)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * removes all registrations
     *
     */
    override fun onDestroy() {
        super.onDestroy()

        unregisterSensor()
        mDataClient = null
        unregisterReceiver(receiver)

        running = false
    }

    /**
     * starts the specific sensor by id
     *
     * @param id - sensor ID
     */
    private fun registerSensor(id: Int) {
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(this)
        val sensor = sensorManager.getDefaultSensor(id)
        Log.i(TAG, "registerSensors: " + sensor?.name)
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        running = true
    }

    /**
     * removes listener to sensor
     *
     */
    private fun unregisterSensor(){

        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(this)
    }


    /**
     * sample of the sensor is put into DataClient through DataMapRequest
     * paths to recognise storage are used from WearOsConstants - the phone part uses them too
     *
     * @param event - sensorEvent
     */
    override fun onSensorChanged(event: SensorEvent) {

        // general path
        val putDataMapReq = PutDataMapRequest.create(SAMPLE_PATH)
        //path to values of the samples
        putDataMapReq.dataMap.putFloatArray(SAMPLE_PATH_VALUE, createArray(event))
        // path to current time
        putDataMapReq.dataMap.putLong(SAMPLE_PATH_TIME, System.currentTimeMillis())

        // sending data
        val putDataReq = putDataMapReq.asPutDataRequest()
        putDataReq.setUrgent()
        mDataClient!!.putDataItem(putDataReq)
    }

    /**
     * packs sensor type, accuracy and values into one floatArray [values, value, ..., sensorType, accuracy]
     *
     * @param event - sensor Event
     * @return floatArray [values, value, ..., sensorType, accuracy]
     */
    private fun createArray(event: SensorEvent): FloatArray {
        return when {
            event.values.size < 3 -> {
                floatArrayOf(event.values[0],
                    event.sensor.type.toFloat(),
                    event.accuracy.toFloat()
                )
            }
            event.values.size < 5 -> {
                floatArrayOf(
                    event.values[0],
                    event.values[1],
                    event.values[2],
                    event.sensor.type.toFloat(),
                    event.accuracy.toFloat()
                )
            }
            else -> {
                floatArrayOf(
                    event.values[0], event.values[1], event.values[2], event.values[3],
                    event.values[4], event.sensor.type.toFloat(), event.accuracy.toFloat()
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    companion object {
        const val TAG = "SensorService"
        const val SENSOR_ID = "SENSOR_ID"
    }
}