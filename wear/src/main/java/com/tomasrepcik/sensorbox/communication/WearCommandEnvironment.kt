package com.tomasrepcik.sensorbox.communication

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.domain.measurement.WearMeasurementPermissionUseCase
import com.tomasrepcik.sensorbox.domain.sensors.GetWearSensorsUseCase
import com.tomasrepcik.sensorbox.domain.sensors.toWearSensorInfo
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearSensorInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface WearRecordingRequirementsUseCase {
    suspend fun validate(request: WearRecordingRequest): AppResult<AppPreferences>

    fun availableSensors(): List<WearSensorInfo>
}

class WearRecordingEnvironment @Inject constructor(
    private val measurementPermissions: WearMeasurementPermissionUseCase,
    private val preferencesRepository: AppPreferencesRepository,
    private val getWearSensors: GetWearSensorsUseCase,
) : WearRecordingRequirementsUseCase {
    override suspend fun validate(request: WearRecordingRequest): AppResult<AppPreferences> {
        val availableSensorIds = getWearSensors().map { it.type }.toSet()
        if (!availableSensorIds.containsAll(request.sensorIds)) {
            return AppResult.failure(AppError(AppErrorCode.VALIDATION, "Validate Wear recording sensors"))
        }

        val missingPermissions = measurementPermissions(request.includesGps)
        if (missingPermissions.isNotEmpty()) {
            return AppResult.failure(
                AppError(
                    code = AppErrorCode.PERMISSION,
                    operation = "Validate Wear recording permissions",
                    diagnosticMessage = "Wear OS is missing required recording permissions",
                    context = mapOf("missingPermissions" to missingPermissions.sorted().joinToString()),
                ),
            )
        }

        return preferencesRepository.preferences.first()
    }

    override fun availableSensors(): List<WearSensorInfo> = getWearSensors().map { sensor -> sensor.toWearSensorInfo() }
}
