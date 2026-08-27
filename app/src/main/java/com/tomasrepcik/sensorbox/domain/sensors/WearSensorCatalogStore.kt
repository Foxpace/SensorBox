package com.tomasrepcik.sensorbox.domain.sensors

import com.tomasrepcik.sensorbox.wearoslib.protocol.WearSensorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearSensorCatalogStore @Inject constructor() {
    private val mutableSensors = MutableStateFlow<List<WearSensorInfo>>(emptyList())
    private val mutableAvailable = MutableStateFlow(false)

    val sensors: StateFlow<List<WearSensorInfo>> = mutableSensors.asStateFlow()
    val isAvailable: StateFlow<Boolean> = mutableAvailable.asStateFlow()

    fun update(sensors: List<WearSensorInfo>) {
        mutableSensors.value = sensors.distinctBy(WearSensorInfo::type).sortedBy(WearSensorInfo::name)
        mutableAvailable.value = true
    }

    fun clear() {
        mutableSensors.value = emptyList()
        mutableAvailable.value = false
    }
}
