package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.Bundle
import com.motionapps.sensorservices.handlers.StorageHandler
import java.io.OutputStream

/**
 * significant motion detector needs to be registered every time after it is triggered
 * that is why, it saves reference to sensorManager
 */
class SignificantMotion : MeasurementInterface, TriggerEventListener() {

    private var outputStream: OutputStream? = null
    private var sensorManager: SensorManager? = null

    /**
     * creates outputStream to save events
     *
     * @param context
     * @param params - requirements from the service like external / internal storage
     */
    override fun initMeasurement(context: Context, params: Bundle) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        outputStream = if(params.getBoolean(MeasurementInterface.INTERNAL_STORAGE)){
            StorageHandler.createFileInInternalFolder(context,
                params.getString(MeasurementInterface.FOLDER_NAME)!!, "significant_motion.csv")
        }else{
            StorageHandler.createFileInFolder(context,
                params.getString(MeasurementInterface.FOLDER_NAME)!!,
                "csv", "significant_motion.csv")
        }

        outputStream?.write("t;event\n".toByteArray())
    }

    /**
     * registers detector
     *
     * @param context
     */
    override fun startMeasurement(context: Context) {
        registerSensor()
    }

    /**
     * cancels registration
     *
     * @param context
     */
    override fun pauseMeasurement(context: Context) {
        sensorManager?.cancelTriggerSensor(this, sensorManager?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION))
    }

    /**
     * saves data to csv file
     *
     * @param context
     */
    override suspend fun saveMeasurement(context: Context) {
        kotlin.runCatching {
            outputStream?.flush()
            outputStream?.close()
        }
    }

    /**
     * saves csv and cancels registration
     *
     * @param context
     */
    override suspend fun onDestroyMeasurement(context: Context) {
        pauseMeasurement(context)
        saveMeasurement(context)
        sensorManager = null
    }

    /**
     * called upon the detection
     *
     * @param triggerEvent - has only value of 1
     */
    override fun onTrigger(triggerEvent: TriggerEvent?) {
        triggerEvent?.let {
            outputStream?.write("${System.currentTimeMillis()};${triggerEvent.values[0]}\n".toByteArray())
        }
        registerSensor()
    }

    /**
     * to register significant motion sensor
     *
     */
    private fun registerSensor(){
        sensorManager?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)?.let{
            sensorManager!!.requestTriggerSensor(this, it)
        }
    }
}