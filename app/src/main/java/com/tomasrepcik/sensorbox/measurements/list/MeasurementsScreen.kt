package com.tomasrepcik.sensorbox.measurements.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxBackScreen
import com.tomasrepcik.sensorbox.measurements.components.measurementArchiveItems

@Composable
fun MeasurementsScreen(
    state: MeasurementsState,
    onIntent: (MeasurementsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SensorBoxBackScreen(
        title = stringResource(R.string.measurements),
        onBack = onBack,
        modifier = modifier,
    ) {
        measurementArchiveItems(state, onIntent)
    }
}
