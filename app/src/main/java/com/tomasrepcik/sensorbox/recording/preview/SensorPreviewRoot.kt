package com.tomasrepcik.sensorbox.recording.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.sensorbox.navigation.FullScreen
import com.tomasrepcik.sensorbox.platform.rememberLocationPreviewPermissionRequest
import com.tomasrepcik.sensorbox.recording.RecordingDevice

@Composable
fun SensorPreviewRoot(
    sensorType: Int?,
    device: RecordingDevice,
    onBack: () -> Unit,
    viewModel: SensorPreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requestLocationPermission = rememberLocationPreviewPermissionRequest(
        onResult = viewModel::handleLocationPreviewPermissionResult,
        onFailure = viewModel::reportFailure,
    )
    LaunchedEffect(viewModel, sensorType, device) {
        viewModel.accept(SensorPreviewIntent.LoadDetails(sensorType, device))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SensorPreviewEffect.RequestLocationPreviewPermission -> requestLocationPermission()
            }
        }
    }
    FullScreen { modifier ->
        SensorPreviewScreen(state, viewModel::accept, onBack, modifier)
    }
}
