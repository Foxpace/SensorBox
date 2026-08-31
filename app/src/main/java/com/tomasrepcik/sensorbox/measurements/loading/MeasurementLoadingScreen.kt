package com.tomasrepcik.sensorbox.measurements.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxBackScreen
import com.tomasrepcik.sensorbox.measurements.components.ArchiveError
import kotlin.math.roundToInt

@Composable
fun MeasurementLoadingScreen(
    state: MeasurementLoadingState,
    onIntent: (MeasurementLoadingIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SensorBoxBackScreen(
        title = state.request?.file?.name ?: stringResource(R.string.measurement_file),
        onBack = {
            onIntent(MeasurementLoadingIntent.Cancel)
            onBack()
        },
        modifier = modifier,
    ) {
        if (state.errorCode != null) {
            item { ArchiveError() }
        } else {
            item { MeasurementLoadingProgress(state.progress) }
        }
    }
}

@Composable
private fun MeasurementLoadingProgress(progress: Float) {
    val boundedProgress = progress.coerceIn(0f, 1f)
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(progress = { boundedProgress })
        Text(
            stringResource(
                R.string.loading_measurement_file_progress,
                (boundedProgress * 100).roundToInt(),
            ),
        )
    }
}
