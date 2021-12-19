package com.motionapps.sensorbox.uiHandlers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.motionapps.sensorservices.types.SensorNeeds
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

@ActivityRetainedScoped
/**
 * Passes sensor data to chart - used in views to show sensor values for short time
 * can by paused and resumed based on the lifecycle
 */
open class GraphUpdater @Inject constructor(): SensorEventListener{

    var chartData: ArrayList<LineGraphSeries<DataPoint>> = ArrayList() // data belongs to chart

    protected var sensorManager: SensorManager? = null
    protected var sensorNeeds: SensorNeeds = SensorNeeds.ACG

    private var maxSensorPoints = -1
    private var startTime = -1L

    protected var running = false

    /**
     * Start the sensor and updates the chart
     *
     * @param context
     * @param sensorNeeds
     */
    open fun startSensing(context: Context, sensorNeeds: SensorNeeds){
        //can be changed in runtime
        restartedFlag = false
        if(sensorNeeds != this.sensorNeeds && running){
            onDestroy()
            registerSensor(context, sensorNeeds)
        }else if(!running){
            registerSensor(context, sensorNeeds)
        }
    }

    /**
     * registration of the sensor, creates beginning timestamp and resets chartData
     *
     * @param context
     * @param sensorNeeds
     */
    private fun registerSensor(context: Context, sensorNeeds: SensorNeeds) {
        startTime = System.currentTimeMillis()
        reducer = System.currentTimeMillis()
        this.sensorNeeds = sensorNeeds

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.getDefaultSensor(this.sensorNeeds.id)?.let {
            maxSensorPoints = (10_000.toDouble()/(it.minDelay.toDouble()/1000.0)).toInt()
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            running = true
        }

        for(i in 0 until this.sensorNeeds.count){
            chartData.add(LineGraphSeries())
        }
    }

    fun onResume(){
        if(!running){
            sensorManager?.getDefaultSensor(sensorNeeds.id)?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                running = true
            }
        }
    }

    private fun restart(){
        for(series in chartData){
            series.resetData(arrayOf(DataPoint(0.0, 0.0)))
        }
        reducer = System.currentTimeMillis()
        startTime = System.currentTimeMillis()
        restartedFlag = false
    }

    fun onPause(){
        if(running){
            sensorManager?.unregisterListener(this)
            running = false
            restartedFlag = false
        }
    }

    fun onDestroy(){
        onPause()
        chartData.clear()
        sensorManager = null
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
    // reduces updates based on time, only one sample per millisecond is allowed
    private var reducer = System.currentTimeMillis()
    private var restartedFlag = false


    override fun onSensorChanged(p0: SensorEvent) {

        if (reducer != System.currentTimeMillis()) {

            if(restartedFlag){
                return
            }

            val actualTime = System.currentTimeMillis()
            if(actualTime <= reducer){
                restartedFlag = true
                restart()
            }

            for (i in 0 until sensorNeeds.count) {
                chartData[i].appendData(
                    DataPoint((actualTime - startTime).toDouble(), p0.values[i].toDouble()
                    ), true, maxSensorPoints
                )
            }
            reducer = System.currentTimeMillis()
        }
    }

}