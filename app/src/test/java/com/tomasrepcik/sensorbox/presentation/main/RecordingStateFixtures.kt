package com.tomasrepcik.sensorbox.presentation.main

import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor

object RecordingStateFixtures {
    fun state(selectedSensorIds: Set<Int> = emptySet(), includesGps: Boolean = false) = RecordingState(
        sensors = listOf(
            SensorDescriptor(type = 1, name = "Accelerometer", vendor = "Fixture"),
        ),
        selectedSensorIds = selectedSensorIds,
        includesGps = includesGps,
    )
}
