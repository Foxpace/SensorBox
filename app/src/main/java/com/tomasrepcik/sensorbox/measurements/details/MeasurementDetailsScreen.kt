package com.tomasrepcik.sensorbox.measurements.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxBackScreen
import com.tomasrepcik.sensorbox.measurements.components.ArchiveError
import com.tomasrepcik.sensorbox.measurements.components.MeasurementLoading

@Composable
fun MeasurementDetailsScreen(
    state: MeasurementDetailsState,
    onIntent: (MeasurementDetailsIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SensorBoxBackScreen(
        title = stringResource(R.string.measurement_details),
        onBack = onBack,
        modifier = modifier,
        itemSpacing = 0.dp,
    ) {
        when {
            state.isLoading -> item { MeasurementLoading(Modifier.fillParentMaxHeight()) }
            state.errorCode != null -> item { ArchiveError() }
            state.details != null -> measurementDetailsItems(state.details, onIntent)
            else -> item { ArchiveError() }
        }
    }
}
