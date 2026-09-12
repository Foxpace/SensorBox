package com.tomasrepcik.sensorbox.pairedrecording

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.recording.WatchRecordingPermissionUseCase
import com.tomasrepcik.sensorbox.recording.sources.GetWatchSensorsUseCase
import com.tomasrepcik.sensorbox.recording.sources.toWatchSensorInfo
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearSensorInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

interface WearRecordingRequirementsUseCase {
    suspend fun validate(request: WearRecordingRequest): AppResult<AppPreferences>

    fun availableSensors(): List<WearSensorInfo>
}

class WearRecordingEnvironment @Inject constructor(
    private val recordingPermissions: WatchRecordingPermissionUseCase,
    private val preferencesRepository: AppPreferencesRepository,
    private val getWatchSensors: GetWatchSensorsUseCase,
) : WearRecordingRequirementsUseCase {
    override suspend fun validate(request: WearRecordingRequest): AppResult<AppPreferences> {
        val availableSensorIds = getWatchSensors().map { it.type }.toSet()
        if (!availableSensorIds.containsAll(request.sensorIds)) {
            return AppResult.failure(AppError(AppErrorCode.VALIDATION, "Validate watch recording sensors"))
        }

        val missingPermissions = recordingPermissions(request.includesGps)
        if (missingPermissions.isNotEmpty()) {
            return AppResult.failure(
                AppError(
                    code = AppErrorCode.PERMISSION,
                    operation = "Validate watch recording permissions",
                    diagnosticMessage = "Wear OS is missing required recording permissions",
                    context = mapOf("missingPermissions" to missingPermissions.sorted().joinToString()),
                ),
            )
        }

        return preferencesRepository.preferences.first()
    }

    override fun availableSensors(): List<WearSensorInfo> = getWatchSensors().map { sensor ->
        sensor.toWatchSensorInfo()
    }
}
