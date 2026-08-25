package com.tomasrepcik.sensorbox.communication

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.domain.measurement.WearMeasurementPermissionUseCase
import com.tomasrepcik.sensorbox.domain.sensors.GetWearSensorsUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearSensorInfo
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
        val missingPermissions = measurementPermissions(request.includesGps)
        if (missingPermissions.isNotEmpty()) {
            return AppResult.failure(AppError(AppErrorCode.PERMISSION, "Prepare Wear recording permissions"))
        }
        return preferencesRepository.preferences.first()
    }

    override fun sensors(): List<WearSensorInfo> = getWearSensors().map { sensor ->
        WearSensorInfo(sensor.type, sensor.name, sensor.vendor)
    }
}
