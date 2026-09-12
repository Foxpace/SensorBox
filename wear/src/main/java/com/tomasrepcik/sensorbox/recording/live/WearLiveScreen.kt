package com.tomasrepcik.sensorbox.recording.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.tomasrepcik.sensorbox.home.WearDashboardIntent
import com.tomasrepcik.sensorbox.home.WearDashboardState

@Composable
internal fun WearLiveScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    val chartModelProducer = remember(state.liveSensorType) { CartesianChartModelProducer() }
    LaunchedEffect(chartModelProducer, state.liveSamples) {
        if (state.liveSamples.isNotEmpty() && state.liveSamples.all { it.isNotEmpty() }) {
            chartModelProducer.runTransaction {
                lineModel {
                    val axisCount = state.liveSamples.minOf { it.size }
                    repeat(axisCount) { axis -> series(state.liveSamples.map { it[axis] }) }
                }
            }
        }
    }
    val selectedSensor = state.sensors.firstOrNull { it.type == state.liveSensorType }
    if (selectedSensor == null) {
        WearSensorPicker(state.sensors, accept)
    } else {
        key(state.liveSensorType) {
            WearLiveSensor(selectedSensor, state.liveSamples.lastOrNull(), chartModelProducer, accept)
        }
    }
}
