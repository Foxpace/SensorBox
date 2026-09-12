package com.tomasrepcik.sensorbox.recording.live

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject

class ObserveSensorValuesUseCase @Inject constructor(@ApplicationContext context: Context) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)

    operator fun invoke(sensorType: Int): Flow<List<Float>> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            close(IllegalArgumentException("Sensor type $sensorType is unavailable"))
            return@callbackFlow
        }
        val listener = sensorListener { value -> trySend(value) }
        val registered = sensorManager.registerListener(
            listener,
            sensor,
            SensorManager.SENSOR_DELAY_UI,
        )
        if (!registered) close(IllegalStateException("Unable to observe ${sensor.name}"))
        awaitClose { sensorManager.unregisterListener(listener) }
    }.conflate()

    private fun sensorListener(onValue: (List<Float>) -> Unit) = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values.isNotEmpty()) onValue(event.values.toList())
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
}
