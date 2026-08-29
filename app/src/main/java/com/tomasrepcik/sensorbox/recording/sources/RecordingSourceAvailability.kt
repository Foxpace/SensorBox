package com.tomasrepcik.sensorbox.recording.sources

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.wearoslib.connection.ObserveWearCapabilityUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_APP_CAPABILITY
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearSensorInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class AvailableRecordingSources(
    val phoneSensors: List<SensorDescriptor>,
    val watchSensors: List<SensorDescriptor> = emptyList(),
    val isWatchConnected: Boolean = false,
)

interface AvailableRecordingSourcesUseCase {
    val current: AvailableRecordingSources

    fun observe(): Flow<AvailableRecordingSources>
}

fun interface ReceiveWatchSensorsUseCase {
    fun receive(sensors: List<WearSensorInfo>)
}

@Singleton
class RecordingSourceAvailability @Inject constructor(
    phoneSensors: AvailableSensorsUseCase,
    private val observeWatchCapability: ObserveWearCapabilityUseCase,
    private val sendWatchCommand: SendWearCommandUseCase,
) : AvailableRecordingSourcesUseCase,
    ReceiveWatchSensorsUseCase {
    private val mutableSources = MutableStateFlow(
        AvailableRecordingSources(phoneSensors = phoneSensors()),
    )

    override val current: AvailableRecordingSources
        get() = mutableSources.value

    override fun observe(): Flow<AvailableRecordingSources> = channelFlow {
        launch { mutableSources.collect { sources -> send(sources) } }
        observeWatchCapability(WEAR_APP_CAPABILITY)
            .catch { error ->
                AppError.from(AppErrorCode.CONNECTIVITY, "Observe Wear connection", error)
                emit(WearConnection.Disconnected)
            }
            .collect(::onWatchConnectionChanged)
    }

    override fun receive(sensors: List<WearSensorInfo>) {
        mutableSources.value = current.copy(
            watchSensors = sensors
                .distinctBy(WearSensorInfo::type)
                .sortedBy(WearSensorInfo::name)
                .map(WearSensorInfo::toSensorDescriptor),
            isWatchConnected = true,
        )
    }

    private suspend fun onWatchConnectionChanged(connection: WearConnection) {
        if (connection is WearConnection.Connected) {
            sendWatchCommand(
                WEAR_APP_CAPABILITY,
                WEAR_MESSAGE_PATH,
                WearCommand.RequestAvailableSensors,
            )
        } else {
            mutableSources.value = current.copy(
                watchSensors = emptyList(),
                isWatchConnected = false,
            )
        }
    }
}
