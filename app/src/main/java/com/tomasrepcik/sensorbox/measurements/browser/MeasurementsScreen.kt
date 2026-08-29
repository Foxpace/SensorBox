package com.tomasrepcik.sensorbox.measurements.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxBackScreen

@Composable
fun MeasurementsScreen(
    state: MeasurementBrowserState,
    onIntent: (MeasurementBrowserIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onIntent(MeasurementBrowserIntent.RefreshMeasurements) }
    SensorBoxBackScreen(
        title = stringResource(R.string.measurements),
        onBack = onBack,
        modifier = modifier,
    ) {
        measurementArchiveItems(state, onIntent)
    }
}
