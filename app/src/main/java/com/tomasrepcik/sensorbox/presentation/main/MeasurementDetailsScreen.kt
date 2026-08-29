package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R

@Composable
fun MeasurementDetailsScreen(
    state: MeasurementBrowserState,
    onIntent: (MeasurementBrowserIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = state.selectedMeasurement
    SensorBoxBackScreen(
        title = stringResource(R.string.measurement_details),
        onBack = onBack,
        modifier = modifier,
        itemSpacing = 0.dp,
    ) {
        if (details == null) {
            item { ArchiveError() }
        } else {
            measurementDetailsItems(details, state, onIntent)
        }
    }
}
