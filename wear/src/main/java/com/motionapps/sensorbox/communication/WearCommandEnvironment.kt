package com.motionapps.sensorbox.communication

import android.hardware.Sensor
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.preferences.AppPreferences
import com.motionapps.sensorbox.core.preferences.AppPreferencesRepository
import com.motionapps.sensorbox.domain.measurement.WearMeasurementPermissionUseCase
import com.motionapps.sensorbox.domain.sensors.GetWearSensorsUseCase
import com.motionapps.wearoslib.protocol.WearRecordingRequest
import com.motionapps.wearoslib.protocol.WearSensorInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface WearCommandEnvironment {
    suspend fun prepare(request: WearRecordingRequest): AppResult<AppPreferences>

    fun sensors(): List<WearSensorInfo>
}

class AndroidWearCommandEnvironment @Inject constructor(
    private val measurementPermissions: WearMeasurementPermissionUseCase,
    private val preferencesRepository: AppPreferencesRepository,
    private val getWearSensors: GetWearSensorsUseCase,
) : WearCommandEnvironment {
    override suspend fun prepare(request: WearRecordingRequest): AppResult<AppPreferences> {
        val availableSensorIds = getWearSensors().map { it.type }.toSet()
        if (!availableSensorIds.containsAll(request.sensorIds)) {
            return AppResult.failure(AppError(AppErrorCode.VALIDATION, "Validate Wear recording sensors"))
        }
        val includesHeartRate = Sensor.TYPE_HEART_RATE in request.sensorIds
        val missingPermissions = measurementPermissions(request.includesGps, includesHeartRate)
        if (missingPermissions.isNotEmpty()) {
            return AppResult.failure(AppError(AppErrorCode.PERMISSION, "Prepare Wear recording permissions"))
        }
        return preferencesRepository.preferences.first()
    }

    override fun sensors(): List<WearSensorInfo> = getWearSensors().map { sensor ->
        WearSensorInfo(sensor.type, sensor.name, sensor.vendor, sensor.isHeartRate)
    }
}
