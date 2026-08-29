package com.tomasrepcik.sensorbox.recording.live

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.tomasrepcik.sensorbox.home.WearDashboardIntent
import com.tomasrepcik.sensorbox.home.WearDashboardState

@Composable
internal fun WearLiveScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    val chartModelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(state.liveSamples) {
        chartModelProducer.runTransaction { lineModel { series(state.liveSamples) } }
    }
    val selectedSensor = state.sensors.firstOrNull { it.type == state.liveSensorType }
    if (selectedSensor == null) {
        WearSensorPicker(state.sensors, accept)
    } else {
        WearLiveSensor(selectedSensor, state.liveSamples.lastOrNull(), chartModelProducer, accept)
    }
}
