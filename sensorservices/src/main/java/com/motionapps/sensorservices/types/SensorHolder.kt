package com.motionapps.sensorservices.types

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlinx.coroutines.*
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
) : SensorEventListener, EndHolder {

    private val lineFormat: String =
        "%d;%d;%s%d\n" // format of the line, %s in created by loop with size of required axes
    private val axes: Int = sensorNeeds.count
    private var isWriting: Boolean = false
    private var buffer: StringBuffer = StringBuffer(10000)
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            withContext(Dispatchers.IO) {
                outputStream.write(sensorNeeds.head.toByteArray())
            }
        }
        scope.launch {
            withContext(Dispatchers.IO){
                while (isActive){
                    delay(10000L)
                    if (queue1.isNotEmpty()){
                        isWriting = true
                        val copy = queue1.toMutableList()
                        queue1.clear()
                        copy.forEach { sensorOutput -> formatLine(sensorOutput) }
                        writeBuffer()
                        isWriting = false
                    }

                    if (queue2.isNotEmpty() && !isWriting){
                        val copy = queue2.toMutableList()
                        queue2.clear()
                        copy.forEach { sensorOutput -> formatLine(sensorOutput) }
                        writeBuffer()
                    }
                }
            }

        }
    }

    private var queue1 = ArrayList<SensorOutput>()
    private var queue2 = ArrayList<SensorOutput>()

    override fun onSensorChanged(event: SensorEvent) {
        if (isWriting){
            queue2.add(SensorOutput(event))
            return
        }
        queue1.add(SensorOutput(event))
    }

    private fun formatLine(sensorOutput: SensorOutput){
        var values = ""
        for (i in 0 until axes) {
            values += sensorOutput.values[i].toString() + ";" // formatting values
        }

        buffer.append(
            lineFormat.format(
                sensorOutput.timestamp,
                sensorOutput.timeStampUnix,
                values,
                sensorOutput.accuracy
            )
        )
    }

    private fun writeBuffer() {
        val bufferToWrite = buffer.toString()
        buffer.setLength(0)
        outputStream.write(bufferToWrite.toByteArray())
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    override suspend fun saveFile() {
        scope.cancel()
        if(isWriting){
            return
        }

        if (queue1.isNotEmpty()){
            withContext(Dispatchers.IO){
                queue1.forEach{ output -> formatLine(output)}
                writeBuffer()
            }
        }

        if (queue2.isNotEmpty()){
            withContext(Dispatchers.IO){
                queue2.forEach{ output -> formatLine(output)}
                writeBuffer()
            }
        }
    }

    data class SensorOutput(
        val timestamp: Long,
        val timeStampUnix: Long,
        val values: FloatArray,
        val accuracy: Int
    ) {

        constructor(sensorOutput: SensorEvent) : this(
            sensorOutput.timestamp,
            System.currentTimeMillis(),
            sensorOutput.values,
            sensorOutput.accuracy
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SensorOutput

            if (timestamp != other.timestamp) return false
            if (accuracy != other.accuracy) return false
            if (!values.contentEquals(other.values)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = timestamp.hashCode()
            result += accuracy.hashCode()
            result = 31 * result + values.contentHashCode()
            return result
        }
    }


}