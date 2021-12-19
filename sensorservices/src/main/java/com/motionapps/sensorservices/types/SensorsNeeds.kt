package com.motionapps.sensorservices.types

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.motionapps.sensorservices.R
import com.motionapps.sensorservices.types.SensorNeeds.Companion.TypeOfRepresentation.PLOT
import com.motionapps.sensorservices.types.SensorNeeds.Companion.TypeOfRepresentation.REALTIME_COUNTER
import com.motionapps.sensorservices.types.SensorNeeds.Companion.TypeOfRepresentation.TEXTVIEW

/**
 * all the sensors and their specifics for formatting and string
 *
 * @property id - of the sensor
 * @property count - of axes
 * @property oneValueTextView - PLOT, TEXTVIEW, COUNTER
 * @property unit - units in which the values should be showed
 * @property head - of the csv
 * @property conversion - conversion rate from metric to imperial - not implemented
 * @property title - for the chart
 */
enum class SensorNeeds( val id: Int, val count: Int, val oneValueTextView: Int,
                        val unit: String, val head: String, private val conversion: Int, val title: Int) {

    ACC(Sensor.TYPE_LINEAR_ACCELERATION, 3, PLOT, UnitsMetricSystem.acceleration, "t;x;y;z;a\n", ImperialConversion.DEBUG, R.string.acc_name),
    ACG(Sensor.TYPE_ACCELEROMETER, 3, PLOT, UnitsMetricSystem.acceleration, "t;x;y;z;a\n", ImperialConversion.DEBUG, R.string.acg_name),
    AGG(Sensor.TYPE_GRAVITY, 3, PLOT, UnitsMetricSystem.acceleration, "t;x;y;z;a\n", ImperialConversion.DEBUG, R.string.agg_name),
    GYRO(Sensor.TYPE_GYROSCOPE, 3, PLOT, UnitsMetricSystem.angle_acceleration, "t;x;y;z;a\n", ImperialConversion.DEBUG, R.string.gyro_name),
    HUM(Sensor.TYPE_RELATIVE_HUMIDITY, 1, PLOT, UnitsMetricSystem.percentage, "t;hum;a\n", ImperialConversion.DEBUG, R.string.humi_name),
    MAGNET(Sensor.TYPE_MAGNETIC_FIELD, 3, PLOT, UnitsMetricSystem.induction, "t;x;y;z;a\n", ImperialConversion.DEBUG, R.string.magnet_name),
    PROXIMITY(Sensor.TYPE_PROXIMITY, 1, TEXTVIEW, UnitsMetricSystem.centimeter, "t;prox;a\n", ImperialConversion.DEBUG, R.string.proxi_name),
    ROTATION(Sensor.TYPE_ROTATION_VECTOR, 4, PLOT, UnitsMetricSystem.nothing, "t;x;y;z;0;a\n", ImperialConversion.DEBUG, R.string.rotation_name),
    PRESSURE(Sensor.TYPE_PRESSURE, 1, PLOT, UnitsMetricSystem.pressure, "t;pressure;a\n", ImperialConversion.DEBUG, R.string.pressure_name),
    LIGHT(Sensor.TYPE_LIGHT, 1, PLOT, UnitsMetricSystem.lux,"t;light;a\n", ImperialConversion.DEBUG, R.string.light_name),
    TEMP(Sensor.TYPE_AMBIENT_TEMPERATURE, 1, PLOT, UnitsMetricSystem.celsius, "t;temp;a\n", ImperialConversion.DEBUG, R.string.temp_name),
    STEP_COUNTER(Sensor.TYPE_STEP_COUNTER, 1, TEXTVIEW, UnitsMetricSystem.steps_string, "t;steps;a\n", ImperialConversion.DEBUG, R.string.step_counter_name),
    STEP_DETECTOR(Sensor.TYPE_STEP_DETECTOR, 1, REALTIME_COUNTER, UnitsMetricSystem.steps_string, "t;steps;a\n", ImperialConversion.DEBUG, R.string.step_detector_name),
    @SuppressLint("InlinedApi")
    HEART_RATE(Sensor.TYPE_HEART_RATE, 1, TEXTVIEW, UnitsMetricSystem.heartrate, "t;bpm;a\n", ImperialConversion.DEBUG, R.string.heart_rate),

    ACC_WEAR(Sensor.TYPE_LINEAR_ACCELERATION, 3, PLOT, UnitsMetricSystem.acceleration, "t;x;y;z;a\n", ImperialConversion.DEBUG, R.string.acc_name_wear),
    ACG_WEAR(Sensor.TYPE_ACCELEROMETER, 3, PLOT, UnitsMetricSystem.acceleration, "t;x;y;z;a\n", ImperialConversion.DEBUG, R.string.acg_name_wear),
    GYRO_WEAR(Sensor.TYPE_GYROSCOPE, 3, PLOT, UnitsMetricSystem.angle_acceleration, "t;x;y;z;a\n", ImperialConversion.DEBUG, R.string.gyro_name_wear),
    MAGNET_WEAR(Sensor.TYPE_MAGNETIC_FIELD, 3, PLOT, UnitsMetricSystem.induction, "t;x;y;z;a\n", ImperialConversion.DEBUG, R.string.magnet_name_wear),

    @SuppressLint("InlinedApi")
    HEART_RATE_WEAR(Sensor.TYPE_HEART_RATE, 1, TEXTVIEW, UnitsMetricSystem.heartrate, "t;bpm;a\n", ImperialConversion.DEBUG, R.string.heart_rate_wear);


    companion object{
        /**
         * iterates through sensors and return list of SensorRequirement
         *
         * @param context
         * @return
         */
        fun getSensors(context: Context): ArrayList<SensorNeeds> {

            val array = ArrayList<SensorNeeds>()
            val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            for(sensorNeeds in values()){
                val sensor: Sensor ?= sensorManager.getDefaultSensor(sensorNeeds.id)
                if(sensor != null && "WEAR" !in sensorNeeds.name){
                    array.add(sensorNeeds)
                }
            }
            return array
        }

        /**
         * searches sensor requirements by id - not for the Wear Os and limited for chart values
         *
         * @param id - of the sensor
         * @return - SensorNeeds
         */
        fun getSensorByIdForChart(id: Int): SensorNeeds {
            for(sensorNeed: SensorNeeds in values()){
                if(sensorNeed.id == id && sensorNeed.oneValueTextView == PLOT && "WEAR" !in sensorNeed.name){
                    return sensorNeed
                }
            }
            return ACG
        }

        /**
         * searches sensor requirements by id - not for the Wear Os
         *
         * @param id - of the sensor
         * @return - SensorNeeds
         */
        fun getSensorById(id: Int): SensorNeeds {
            for(sensorNeed: SensorNeeds in values()){
                if(sensorNeed.id == id && "WEAR" !in sensorNeed.name){
                    return sensorNeed
                }
            }
            return ACG
        }

        /**
         * searches sensor requirements by id - specified for Wear Os
         *
         * @param id - of the sensor
         * @return - SensorNeeds
         */
        fun getSensorByIdWearOs(id: Int): SensorNeeds {
            for(sensorNeed: SensorNeeds in values()){
                if(sensorNeed.id == id && (sensorNeed.oneValueTextView == PLOT || sensorNeed.oneValueTextView == TEXTVIEW) && "WEAR" in sensorNeed.name){
                    return sensorNeed
                }
            }
            return ACG_WEAR
        }

        const val GPS: String = "GPS"

        object TypeOfRepresentation{
            const val PLOT = 0
            const val TEXTVIEW = 1
            const val REALTIME_COUNTER = 3
        }

        object UnitsMetricSystem{
            const val acceleration =  "m/s2"
            const val angle_acceleration = "rad/s"
            const val percentage = "%"
            const val induction = "μT"
            const val centimeter = "cm"
            const val nothing = "-"
            const val pressure = "hPa"
            const val lux = "lx"
            const val celsius = "°C"
            const val steps_string = "steps"
            const val heartrate = "bpm"
        }

        object ImperialConversion{
            const val DEBUG = 1
            //TODO create Imperial conversion
        }
    }


}


