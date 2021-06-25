package com.motionapps.sensorbox.uiHandlers

import android.content.Context
import com.motionapps.sensorservices.types.SensorNeeds
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject


/**
 * Works with HomeFragment - stores sensors of the devices, which will be measured
 *
 * @param context
 */
@ViewModelScoped
class SensorViewHandler @Inject constructor(@ActivityContext context: Context) {

    val sensorsToRecord: HashMap<Int, Boolean> = HashMap()
    val sensorsWearOsToRecord: HashMap<Int, Boolean> = HashMap()
    var gpsMeasurement: Boolean = false

    /**
     * at the beginning - all sensors are set false - not to measure
     */
    init {
        for (sensor: SensorNeeds in SensorNeeds.Companion.getSensors(context)) {
            sensorsToRecord[sensor.id] = false
        }
    }

    /**
     * @return - true, if at least one sensor is marked to measure
     */
    fun isSomethingToMeasure(): Boolean {
        for(key: Int in sensorsToRecord.keys){ // phone sensors
            sensorsToRecord[key]?.let {
                if(it){
                    return true
                }
            }
        }

        for(key: Int in sensorsWearOsToRecord.keys){ // wear os sensors
            sensorsWearOsToRecord[key]?.let {
                if(it){
                    return true
                }
            }
        }

        return gpsMeasurement
    }

    /**
     * Phone sensors to measure
     *
     * @return - ids of the sensors
     */
    fun getSensorsToMeasure(): IntArray {
        val sensors = ArrayList<Int>()
        for(key: Int in sensorsToRecord.keys){
            if(sensorsToRecord[key]!!){
                sensors.add(key)
            }
        }

        removeActiveMeasurements()

        return sensors.toIntArray()
    }

    /**
     * Wear Os
     *
     * @return ids of the wear os sensors to measure
     */
    fun getWearOsToMeasure(): IntArray? {
        val sensors = ArrayList<Int>()
        for(key: Int in sensorsWearOsToRecord.keys){
            if(sensorsWearOsToRecord[key]!!){
                sensors.add(key)
            }
        }

        removeActiveWearOsMeasurements()

        if(sensors.isEmpty()){
            return null
        }

        return sensors.toIntArray()
    }

    /**
     * all sensors are turned to false
     *
     */
    private fun removeActiveMeasurements() {
        for(key: Int in sensorsToRecord.keys){
            sensorsToRecord[key] = false
        }
    }

    /**
     * all Wear Os sensors are turned to false
     *
     */
    fun removeActiveWearOsMeasurements() {
        for(key: Int in sensorsWearOsToRecord.keys){
            sensorsWearOsToRecord[key] = false
        }
    }

    /**
     * Wear Os sensors can be added through sensorNeeds
     *
     * @param sensorNeeds
     */
    fun addWearOsSensor(sensorNeeds: SensorNeeds) {
        if(!sensorsWearOsToRecord.containsKey(sensorNeeds.id)){
            sensorsWearOsToRecord[sensorNeeds.id] = false
        }
    }


}