package com.motionapps.sensorbox.uiHandlers

import android.content.Context
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.motionapps.sensorservices.types.SensorNeeds
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

@FragmentScoped
/**
 * Same as Graphupdater
 * But updates text view, not GraphView
 */
class TextUpdater @Inject constructor(): GraphUpdater(){

    private var counter = 0.0

    var onTextUpdater: TextUpdaterInterface? = null

    interface TextUpdaterInterface{
        fun onTextUpdate(s: String)
    }


    override fun startSensing(context: Context, sensorNeeds: SensorNeeds){
        this.sensorNeeds = sensorNeeds
        this.sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.getDefaultSensor(sensorNeeds.id)?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            running = true
        }
    }

    override fun onSensorChanged(p0: SensorEvent) {
        val text = if (sensorNeeds.oneValueTextView == SensorNeeds.Companion.TypeOfRepresentation.TEXTVIEW) {
            "%.2f\n%s".format(p0.values[0], sensorNeeds.unit)
        } else {
            counter += p0.values[0]
            "%.2f\n%s".format(counter, sensorNeeds.unit)
        }
        onTextUpdater?.onTextUpdate(text)
    }

}