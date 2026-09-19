package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor

object RecordingStateFixtures {
    fun state(selectedSensorIds: Set<Int> = emptySet(), includesGps: Boolean = false) = RecordingState(
        sensors = listOf(
            SensorDescriptor(type = 1, name = "Accelerometer", vendor = "Fixture"),
        ),
        selectedSensorIds = selectedSensorIds,
        includesGps = includesGps,
    )
}
