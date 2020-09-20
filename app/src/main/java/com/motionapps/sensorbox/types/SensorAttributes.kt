package com.motionapps.sensorbox.types

import android.content.Context
import android.hardware.Sensor
import android.os.Build
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.fragments.SensorInfoView
import com.motionapps.sensorservices.types.SensorNeeds

object SensorAttributes {

    /**
     * Creates list of attributes from methods of the Sensor object
     *
     * @param context
     * @param sensor - to get attributes
     * @param icon - icon from the SensorResources
     * @param sensorNeeds - other information for the sensor, specific for the UI and formatting
     * @return list of attributes to inflate into appropriate view
     */
    fun getSensorInfoViews(context: Context, sensor: Sensor, icon: Int, sensorNeeds: SensorNeeds): ArrayList<SensorInfoView> {

        // name of the sensor
        val sensorInfo = arrayListOf(
            SensorInfoView(
                context.getString(R.string.sensor_info_name),
                sensor.name.replace(" ", "\n").replace("-", "\n"),
                icon
            ),

            // version
            SensorInfoView(
                context.getString(R.string.sensor_info_version),
                sensor.version.toString(),
                R.drawable.ic_version
            ),

            // vendor
            SensorInfoView(
                context.getString(R.string.sensor_info_vendor),
                sensor.vendor.replace(" ", "\n").replace("-", "\n"),
                R.drawable.ic_vendor
            ),

            // units
            SensorInfoView(
                "%s\n[%s]".format(
                    context.getString(R.string.sensor_info_resolution),
                    sensorNeeds.unit
                ), sensor.resolution.toString(), R.drawable.ic_resolution
            ),

            // power consumption [mA]
            SensorInfoView(
                context.getString(R.string.sensor_info_power),
                "%.2f".format(sensor.power),
                R.drawable.ic_power
            ),

            // maximum range
            SensorInfoView(
                "%s [%s]".format(
                    context.getString(R.string.sensor_info_max_range),
                    sensorNeeds.unit
                ), "%.2f".format(sensor.maximumRange), R.drawable.ic_maxvalue
            ),

            // minimal delay [µs]
            SensorInfoView(
                context.getString(R.string.sensor_info_min_delay),
                sensor.minDelay.toString(),
                R.drawable.ic_mindelay
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // maximal delay [µs] - available only above LOLLIPOP
            sensorInfo.add(
                SensorInfoView(
                    context.getString(R.string.sensor_info_max_delay),
                    sensor.maxDelay.toString(),
                    R.drawable.ic_maxdelay
                )
            )
        }

        return sensorInfo
    }

    /**
     * list of strings represents data passed from Wear Os
     * -1 - data is not available
     * sensor id | name | version | vendor | resolution | power | maximal range | minimal range | maximal range
     * sensor id is not shown
     * @param context
     * @param data - list of attributes
     * @param icon - of the sensor
     * @param sensorNeeds - required sensor - formatting information
     * @return list of attributes to inflate into appropriate view
     */
    fun getViewSensorInfoWearOs(context: Context, data: List<String>, icon: Int, sensorNeeds: SensorNeeds): ArrayList<SensorInfoView> {
        val sensorInfo = ArrayList<SensorInfoView>()

        for (i in 1..8) {
            //name
            if (i == 1 && data[i] != "-1") {
                sensorInfo.add(
                    SensorInfoView(
                        context.getString(R.string.sensor_info_name),
                        data[1].replace(" ", "\n").replace("-", "\n"),
                        icon
                    )
                )
            } else if (i == 2 && data[i] != "-1") { // version
                sensorInfo.add(
                    SensorInfoView(
                        context.getString(R.string.sensor_info_version),
                        data[2],
                        R.drawable.ic_version
                    )
                )
            } else if (i == 3 && data[i] != "-1") { // vendor
                sensorInfo.add(
                    SensorInfoView(
                        context.getString(R.string.sensor_info_vendor),
                        data[3].replace(" ", "\n").replace("-", "\n"),
                        R.drawable.ic_vendor
                    )
                )

            } else if (i == 4 && data[i] != "-1") { // resolution
                sensorInfo.add(
                    SensorInfoView(
                        "%s\n[%s]".format(
                            context.getString(R.string.sensor_info_resolution),
                            sensorNeeds.unit
                        ), data[4], R.drawable.ic_resolution
                    )
                )
            } else if (i == 5 && data[i] != "-1") { // power
                sensorInfo.add(
                    SensorInfoView(
                        context.getString(R.string.sensor_info_power),
                        "%.2f".format(data[5].toFloat()),
                        R.drawable.ic_power
                    )
                )
            } else if (i == 6 && data[i] != "-1") { // maximal range
                sensorInfo.add(
                    SensorInfoView(
                        "%s [%s]".format(
                            context.getString(R.string.sensor_info_max_range),
                            sensorNeeds.unit
                        ), "%.2f".format(data[6].toFloat()), R.drawable.ic_maxvalue
                    )
                )
            } else if (i == 7 && data[i] != "-1") { // minimal delay
                sensorInfo.add(
                    SensorInfoView(
                        context.getString(R.string.sensor_info_min_delay),
                        data[7],
                        R.drawable.ic_mindelay
                    )
                )
            } else if (i == 8 && data[i] != "-1") { // maximal delay
                sensorInfo.add(
                    SensorInfoView(
                        context.getString(R.string.sensor_info_max_delay),
                        data[8],
                        R.drawable.ic_maxdelay
                    )
                )
            }
        }

        return sensorInfo

    }

}