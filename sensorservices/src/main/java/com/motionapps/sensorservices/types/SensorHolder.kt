package com.motionapps.sensorservices.types

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream

/**
 *  registers specific sensor and saves it to CSV file
 *
 * @property sensorId - if of the sensor to register
 * @property outputStream - appropriate file outputStream
 *
 * @param sensorNeeds -
 */
class SensorHolder(
    val sensorId: Int,
    sensorNeeds: SensorNeeds,
    private val outputStream: OutputStream
): SensorEventListener, EndHolder {

    private val lineFormat: String = "%d;%s%d\n" // format of the line, %s in created by loop with size of required axes
    private val axes: Int = sensorNeeds.count

    init {
        outputStream.write(sensorNeeds.head.toByteArray())
    }

    override fun onSensorChanged(p0: SensorEvent) {

        var values = ""
        for (i in 0 until axes) {
            values += p0.values[i].toString() + ";" // formatting values
        }
        CoroutineScope(Dispatchers.IO).launch {
            kotlin.runCatching {
                outputStream.write(lineFormat.format(p0.timestamp, values, p0.accuracy).toByteArray())
            }
        }

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override fun saveFile() {
        outputStream.flush()
        outputStream.close()
    }


}