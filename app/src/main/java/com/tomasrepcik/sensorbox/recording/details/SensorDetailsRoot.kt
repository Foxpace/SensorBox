package com.tomasrepcik.sensorbox.recording.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.tomasrepcik.sensorbox.navigation.FullScreen
import com.tomasrepcik.sensorbox.navigation.SensorPreviewRoute
import com.tomasrepcik.sensorbox.platform.rememberLocationPreviewPermissionRequest
import com.tomasrepcik.sensorbox.recording.RecordingDevice

@Composable
fun SensorDetailsRoot(
    sensorType: Int?,
    device: RecordingDevice,
    onNavigate: (NavKey) -> Unit,
    onBack: () -> Unit,
    viewModel: SensorDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requestLocationPermission = rememberLocationPreviewPermissionRequest(
        onResult = viewModel::handleLocationPreviewPermissionResult,
        onFailure = viewModel::reportFailure,
    )
    LaunchedEffect(viewModel, sensorType, device) {
        viewModel.accept(SensorDetailsIntent.LoadDetails(sensorType, device))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SensorDetailsEffect.RequestLocationPreviewPermission -> requestLocationPermission()

                is SensorDetailsEffect.OpenSensorPreview -> onNavigate(
                    SensorPreviewRoute(effect.sensorType, effect.device),
                )
            }
        }
    }
    FullScreen { modifier ->
        SensorDetailsScreen(
            state = state,
            onBack = onBack,
            onPreview = { viewModel.accept(SensorDetailsIntent.OpenSensorPreview) },
            onIntent = viewModel::accept,
            modifier = modifier,
        )
    }
}
