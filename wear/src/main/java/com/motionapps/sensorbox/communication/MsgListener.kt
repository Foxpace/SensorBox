package com.motionapps.sensorbox.communication

import android.hardware.Sensor
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.suspendAppResult
import com.motionapps.sensorbox.core.error.suspendFlatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorbox.core.preferences.AppPreferencesRepository
import com.motionapps.sensorbox.domain.measurement.WearMeasurementControlUseCase
import com.motionapps.sensorbox.domain.measurement.WearMeasurementPermissionUseCase
import com.motionapps.sensorbox.domain.sensors.GetWearSensorsUseCase
import com.motionapps.wearoslib.WearOsConstants.PHONE_APP_CAPABILITY
import com.motionapps.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.motionapps.wearoslib.WearOsConstants.WEAR_MESSAGE_PATH
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
import com.motionapps.wearoslib.protocol.WearSensorInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MsgListener : WearableListenerService() {
    @Inject
    lateinit var measurementControl: WearMeasurementControlUseCase

    @Inject
    lateinit var measurementPermissions: WearMeasurementPermissionUseCase

    @Inject
    lateinit var preferencesRepository: AppPreferencesRepository

    @Inject
    lateinit var getWearSensors: GetWearSensorsUseCase

    @Inject
    lateinit var sendWearMessage: SendWearMessageUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WEAR_MESSAGE_PATH) return
        val command = WearCommandCodec.decode(messageEvent.data).fold(
            onSuccess = { it },
            onFailure = { return },
        )
        when (command) {
            is WearCommand.StartMeasurement -> startMeasurement(command)
            WearCommand.StopMeasurement -> measurementControl.stop()
            WearCommand.RequestSensorList -> sendSensorList()
            WearCommand.SyncMeasurements, WearCommand.LaunchPhone, is WearCommand.SensorList -> Unit
        }
    }

    private fun sendSensorList() {
        serviceScope.launch {
            suspendAppResult(AppError.Kind.MEASUREMENT, "Read Wear sensors") {
                val sensors = getWearSensors().map { sensor ->
                    WearSensorInfo(sensor.type, sensor.name, sensor.vendor, sensor.isHeartRate)
                }
                sensors
            }.suspendFlatMap { sensors ->
                WearCommandCodec.encode(WearCommand.SensorList(sensors))
            }.suspendFlatMap { payload ->
                sendWearMessage(
                    PHONE_APP_CAPABILITY,
                    PHONE_MESSAGE_PATH,
                    payload,
                )
            }.withAppError(AppError.Kind.CONNECTIVITY, "Send Wear sensor list")
        }
    }

    private fun startMeasurement(command: WearCommand.StartMeasurement) {
        serviceScope.launch {
            suspendAppResult(AppError.Kind.PERMISSION, "Check remote measurement permissions") {
                val includesHeartRate = Sensor.TYPE_HEART_RATE in command.sensorIds
                measurementPermissions(command.includesGps, includesHeartRate)
            }.suspendFlatMap { missingPermissions ->
                if (missingPermissions.isNotEmpty()) {
                    return@suspendFlatMap Result.failure(
                        AppError(AppError.Kind.PERMISSION, "Start remote Wear measurement"),
                    )
                }
                preferencesRepository.preferences.first().suspendFlatMap { preferences ->
                    measurementControl.start(
                        sensorIds = command.sensorIds.toSet(),
                        includesGps = command.includesGps,
                        preferences = preferences,
                        folderName = command.folderName,
                        startAtEpochMillis = command.startAtEpochMillis,
                        durationMillis = command.durationMillis,
                        measurementType = command.measurementType,
                    )
                }
            }.withAppError(AppError.Kind.MEASUREMENT, "Handle remote measurement start")
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
