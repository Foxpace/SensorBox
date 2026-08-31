package com.tomasrepcik.sensorbox.measurements.loading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MeasurementLoadingRoot(
    measurementId: String,
    fileId: String,
    onOpenPreview: (measurementId: String, fileId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasurementLoadingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MeasurementLoadingEffect.OpenPreview -> {
                    onOpenPreview(effect.measurementId, effect.fileId)
                }
            }
        }
    }
    LaunchedEffect(viewModel, measurementId, fileId) {
        viewModel.accept(MeasurementLoadingIntent.Load(measurementId, fileId))
    }
    MeasurementLoadingScreen(state, viewModel::accept, onBack, modifier)
}
