package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.tomasrepcik.sensorbox.navigation.FullScreen
import com.tomasrepcik.sensorbox.navigation.MainRoute

@Composable
fun ActiveRecordingRoot(onNavigate: (NavKey) -> Unit, viewModel: ActiveRecordingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ActiveRecordingEffect.RecordingStopped -> onNavigate(MainRoute.RECORD)
            }
        }
    }
    FullScreen { modifier ->
        ActiveRecordingScreen(state, viewModel::accept, modifier)
    }
}
