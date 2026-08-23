package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.os.Bundle

/**
 * basic methods, which handlers have to call them at once with service lifecycle events
 *
 */
interface MeasurementInterface {

    fun initMeasurement(context: Context, params: Bundle): Result<Unit>
    fun startMeasurement(context: Context): Result<Unit>
    fun pauseMeasurement(context: Context): Result<Unit>
    suspend fun saveMeasurement(context: Context): Result<Unit>
    suspend fun onDestroyMeasurement(context: Context): Result<Unit>

    companion object {
        // keys for the bundle in service to handlers
        const val FOLDER_NAME = "FOLDER_NAME"
        const val SENSOR_ID = "SENSOR_ID"
        const val SENSOR_SPEED = "SENSOR_SPEED"
        const val INTERNAL_STORAGE = "INTERNAL_STORAGE"
    }
}
