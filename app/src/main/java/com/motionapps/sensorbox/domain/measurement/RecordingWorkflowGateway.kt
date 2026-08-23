package com.motionapps.sensorbox.domain.measurement

import android.content.Intent
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.domain.sensors.GetAvailableSensorsUseCase
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
import javax.inject.Inject

interface RecordingWorkflowGateway {
    fun sensors(): List<SensorDescriptor>

    fun storagePath(): String?

    fun hasStorage(): Boolean

    fun persistStorage(resultIntent: Intent?): AppResult<Unit>

    fun missingPermissions(request: MeasurementRequest): Set<String>

    suspend fun start(request: MeasurementRequest): AppResult<Unit>

    suspend fun stop(): AppResult<Unit>

    fun annotate(text: String): AppResult<Unit>
}

class AndroidRecordingWorkflowGateway @Inject constructor(
    private val getAvailableSensors: GetAvailableSensorsUseCase,
    private val documentStorage: DocumentStorageUseCase,
    private val measurementPermissions: MeasurementPermissionUseCase,
    private val measurementControl: MeasurementControlUseCase,
) : RecordingWorkflowGateway {
    override fun sensors(): List<SensorDescriptor> = getAvailableSensors()

    override fun storagePath(): String? = documentStorage.displayPath().getOrNull()

    override fun hasStorage(): Boolean = documentStorage.hasStorage().getOrNull() == true

    override fun persistStorage(resultIntent: Intent?): AppResult<Unit> = resultIntent
        ?.let(documentStorage::persist)
        ?: AppResult.failure(AppError(AppErrorCode.STORAGE, "Select recording storage directory"))

    override fun missingPermissions(request: MeasurementRequest): Set<String> =
        measurementPermissions.missingPermissions(request)

    override suspend fun start(request: MeasurementRequest): AppResult<Unit> = measurementControl.start(request)

    override suspend fun stop(): AppResult<Unit> = measurementControl.stop()

    override fun annotate(text: String): AppResult<Unit> = measurementControl.annotate(text)
}
