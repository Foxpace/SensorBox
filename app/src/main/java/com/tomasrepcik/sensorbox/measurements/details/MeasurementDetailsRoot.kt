package com.tomasrepcik.sensorbox.measurements.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MeasurementDetailsRoot(
    measurementId: String,
    onOpenFile: (measurementId: String, fileId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasurementDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MeasurementDetailsEffect.OpenFile -> onOpenFile(effect.measurementId, effect.fileId)
            }
        }
    }
    LaunchedEffect(viewModel, measurementId) {
        viewModel.accept(MeasurementDetailsIntent.Load(measurementId))
    }
    MeasurementDetailsScreen(state, viewModel::accept, onBack, modifier)
}
