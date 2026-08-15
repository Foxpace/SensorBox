package com.motionapps.sensorbox.presentation.main

import com.motionapps.sensorbox.domain.sensors.SensorDescriptor

object MainStateFixtures {
    fun state(
        route: MainRoute = MainRoute.RECORD,
        selectedSensorIds: Set<Int> = emptySet(),
        includesGps: Boolean = false,
    ) = MainState(
        route = route,
        sensors = listOf(
            SensorDescriptor(type = 1, name = "Accelerometer", vendor = "Fixture", isHeartRate = false),
        ),
        selectedSensorIds = selectedSensorIds,
        includesGps = includesGps,
    )
}
