package com.tomasrepcik.sensorbox.measurements.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MeasurementPreviewRoot(
    measurementId: String,
    fileId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MeasurementPreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, measurementId, fileId) {
        viewModel.accept(MeasurementPreviewIntent.Load(measurementId, fileId))
    }
    MeasurementPreviewScreen(state, viewModel::accept, onBack, modifier)
}
