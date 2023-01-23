package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.FOLDER_NAME
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.INTERNAL_STORAGE
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.SENSOR_ID
import com.motionapps.sensorservices.handlers.measurements.MeasurementInterface.Companion.SENSOR_SPEED
import com.motionapps.sensorservices.types.EndHolder
import com.motionapps.sensorservices.types.SensorHolder
import com.motionapps.sensorservices.types.SensorNeeds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream


/**
 * manages SensorHolders to store sensor samples into seperate csv files
 *
 */
class SensorMeasurement: MeasurementInterface {

    // stores all the data with adequate eventListener
    private val holders: ArrayList<SensorHolder> = ArrayList()
    private lateinit var params: Bundle

    /**
     * creates all the holders with adequate outputStreams and sensor to register
     *
     * @param context
     * @param params - from service as bundle
     */
    override fun initMeasurement(context: Context, params: Bundle) {
        this.params = params

        for(sensorId: Int in params.getIntArray(SENSOR_ID)!!){
            val sensorNeeds: SensorNeeds = SensorNeeds.getSensorById(sensorId)

            val outputStream: OutputStream? = if (params.getBoolean(INTERNAL_STORAGE)) {
                StorageHandler.createFileInInternalFolder(
                    context,
                    params.getString(FOLDER_NAME)!!,
                    "$sensorNeeds.csv"
                )
            } else {
                StorageHandler.createFileInFolder(
                    context, params.getString(FOLDER_NAME)!!, "csv", "$sensorNeeds.csv"
                )
            }

            outputStream?.let {
                holders.add(SensorHolder(sensorId, sensorNeeds, it))
            }
        }
    }

    /**
     * sensors are registered with required speed
     *
     * @param context
     */
    override fun startMeasurement(context: Context) {
        val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        for(holder: SensorHolder in holders){
            val sensor: Sensor = sensorManager.getDefaultSensor(holder.sensorId)
            sensorManager.registerListener(holder, sensor, this.params.getInt(SENSOR_SPEED))
        }
    }

    /**
     * unregisters holders
     *
     * @param context
     */
    override fun pauseMeasurement(context: Context) {
        val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        for(holder: SensorHolder in holders){
            sensorManager.unregisterListener(holder)
        }

    }

    /**
     * saves csv files
     *
     * @param context
     */
    override suspend fun saveMeasurement(context: Context) {
        for(holder: EndHolder in holders){
            holder.saveFile()
        }
        holders.removeAll(holders.toSet())
    }

    /**
     * saves and unregisters all the sensors
     *
     * @param context
     */
    override suspend fun onDestroyMeasurement(context: Context) {
        withContext(Dispatchers.Main){
            pauseMeasurement(context)
        }

        withContext(Dispatchers.IO){
            saveMeasurement(context)
        }
    }
}