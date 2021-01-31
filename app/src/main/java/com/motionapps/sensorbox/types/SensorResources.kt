package com.motionapps.sensorbox.types

import com.motionapps.sensorbox.R

/**
 * Resources for different sensors
 *
 * @property icon - custom icon for every sensor
 * @property title - name of the property, which sensor measures
 */
enum class SensorResources(val icon: Int, val title: Int) {
    ACG(R.drawable.ic_acceleration_icon, R.string.acg_name),
    ACC(R.drawable.ic_linear_acceleration_icon, R.string.acc_name),
    AGG(R.drawable.ic_gravity_icon, R.string.agg_name),
    GYRO(R.drawable.ic_gyroscope_icon, R.string.gyro_name),
    HUM(R.drawable.ic_water_drop, R.string.humi_name),
    MAGNET(R.drawable.ic_magnet, R.string.magnet_name),
    PROXIMITY(R.drawable.ic_proximity, R.string.proxi_name),
    ROTATION(R.drawable.ic_rotation_icon, R.string.rotation_name),
    PRESSURE(R.drawable.ic_pressure, R.string.pressure_name),
    LIGHT(R.drawable.ic_light, R.string.light_name),
    TEMP(R.drawable.ic_temperature, R.string.temp_name),
    STEP_COUNTER(R.drawable.ic_steps, R.string.step_counter_name),
    STEP_DETECTOR(R.drawable.ic_steps_detector,R.string.step_detector_name),
    ACG_WEAR(R.drawable.ic_acceleration_icon, R.string.acg_name_wear),
    ACC_WEAR(R.drawable.ic_linear_acceleration_icon, R.string.acc_name_wear),
    GYRO_WEAR(R.drawable.ic_gyroscope_icon, R.string.gyro_name_wear),
    MAGNET_WEAR(R.drawable.ic_magnet, R.string.magnet_name_wear),
    HEART_RATE_WEAR(R.drawable.ic_heart_rate, R.string.heart_rate_wear);
}