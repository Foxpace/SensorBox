package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.os.Bundle

/**
 * basic methods, which handlers have to call them at once with service lifecycle events
 *
 */
interface MeasurementInterface {

    fun initMeasurement(context: Context, params: Bundle)
    fun startMeasurement(context: Context)
    fun pauseMeasurement(context: Context)
    fun saveMeasurement(context: Context)
    fun onDestroyMeasurement(context: Context)

    companion object {
        // keys for the bundle in service to handlers
        const val FOLDER_NAME = "FOLDER_NAME"
        const val SENSOR_ID = "SENSOR_ID"
        const val SENSOR_SPEED = "SENSOR_SPEED"
        const val INTERNAL_STORAGE = "INTERNAL_STORAGE"
    }
}

