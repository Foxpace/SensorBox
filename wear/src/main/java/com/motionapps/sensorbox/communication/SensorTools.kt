package com.motionapps.sensorbox.communication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.motionapps.wearoslib.WearOsConstants.WEAR_SEND_SENSOR_INFO
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*

/**
 * Aggregates data about sensors into one string, which can be sent to Phone
 * Works only with certain Sensors
 * main message and content are separated by ;
 * sensors alone are divided by \n
 * attributes of the single sensors are divided by |
 *
 * message;name|version|... \n
 * name|version|...\n
 *
 * value -1 represents missing information
 */
object SensorTools {
    private const val TAG = "SensorTools"

    private val sensorTypes = intArrayOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_LINEAR_ACCELERATION,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_MAGNETIC_FIELD,
        Sensor.TYPE_HEART_RATE
    )

    /**
     * iterates through available sensors in Wear Os
     *
     * @param context
     * @return
     */
    fun getSensorInfo(context: Context): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append(WEAR_SEND_SENSOR_INFO).append(";") // adding main message for phone

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        for (id in sensorTypes) {
            val sensor = sensorManager.getDefaultSensor(id)
            if (sensor != null) {
                stringBuilder.append(aboutSensor(id, sensor)).append("\n") // gets all information about sensor
            }
        }
        return stringBuilder.toString()
    }

    fun isHeartRatePermissionRequired(context: Context): Boolean{
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if(sensor != null){
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_DENIED
        }
        return false
    }

    /**
     * iterates through all methods of the sensor and obtains all attributes
     *
     * @param id - of the sensor
     * @param sensor - object to get attributes
     * @return string with attributes of the sensor divided by |
     */
    private fun aboutSensor(id: Int, sensor: Sensor): String {
        var method: Method
        var o: Any?
        val stringBuilder = StringBuilder()
        stringBuilder.append(id).append("|")

        // methods
        val methods: ArrayList<String> = object : ArrayList<String>() {
            init {
                add("getName")
                add("getVersion")
                add("getVendor")
                add("getResolution")
                add("getPower")
                add("getMaximumRange")
                add("getMinDelay")
                add("getMaxDelay")
            }
        }

        // iteration through methods
        for (i in methods.indices) {
            try {
                method = sensor.javaClass.getMethod(methods[i])
                o = method.invoke(sensor)
            } catch (e: NoSuchMethodException) {
                Log.e(TAG, "Method does not exist: " + methods[i])
                o = "-1"
            } catch (e: IllegalAccessException) {
                Log.e(TAG, "Method is no accessible: " + methods[i])
                o = "-1"
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
                o = "-1"
            }
            stringBuilder.append(o).append("|")
        }

        return stringBuilder.toString()
    }
}