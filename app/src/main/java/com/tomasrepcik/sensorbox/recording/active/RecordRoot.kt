package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.tomasrepcik.sensorbox.measurements.sync.WatchSyncRoot
import com.tomasrepcik.sensorbox.navigation.FullScreen
import com.tomasrepcik.sensorbox.navigation.RecordingSetupRoute
import com.tomasrepcik.sensorbox.navigation.SensorDetailsRoute

@Composable
fun RecordRoot(onNavigate: (NavKey) -> Unit, viewModel: RecordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is RecordEffect.Navigate -> onNavigate(effect.route)

                is RecordEffect.OpenSensorDetails -> onNavigate(
                    SensorDetailsRoute(effect.sensorType, effect.device),
                )

                is RecordEffect.OpenRecordingSetup -> onNavigate(RecordingSetupRoute(effect.draft))
            }
        }
    }
    FullScreen { modifier ->
        RecordScreen(state, viewModel::accept, modifier) {
            WatchSyncRoot(
                isWatchConnected = state.isWatchConnected,
            )
        }
    }
}
