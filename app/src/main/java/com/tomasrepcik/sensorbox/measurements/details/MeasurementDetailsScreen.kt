package com.tomasrepcik.sensorbox.measurements.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxBackScreen
import com.tomasrepcik.sensorbox.measurements.browser.ArchiveError
import com.tomasrepcik.sensorbox.measurements.browser.MeasurementBrowserIntent
import com.tomasrepcik.sensorbox.measurements.browser.MeasurementBrowserState

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
