package com.tomasrepcik.sensorbox.measurements.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxBackScreen
import com.tomasrepcik.sensorbox.design.SensorBoxSettingsSection
import com.tomasrepcik.sensorbox.measurements.components.ArchiveError
import com.tomasrepcik.sensorbox.measurements.components.MetadataRow
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileContent
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementMetadataEntry
import kotlin.math.roundToInt

@Composable
fun MeasurementPreviewScreen(
    state: MeasurementPreviewState,
    onIntent: (MeasurementPreviewIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SensorBoxBackScreen(
        title = state.file?.name ?: stringResource(R.string.measurement_file),
        onBack = onBack,
        modifier = modifier,
    ) {
        when {
            state.isLoading -> item {
                MeasurementPreviewProgress(
                    progress = state.progress,
                    modifier = Modifier.fillParentMaxHeight(),
                )
            }

            state.errorCode != null -> item { ArchiveError() }

            else -> measurementPreviewItems(
                state.content,
                state.sensorMetadata,
                state.sensorChartTimeWindow,
                onIntent,
            )
        }
    }
}

@Composable
private fun MeasurementPreviewProgress(progress: Float, modifier: Modifier = Modifier) {
    val boundedProgress = progress.coerceIn(0f, 1f)
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
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

private fun LazyListScope.measurementPreviewItems(
    content: MeasurementFileContent?,
    sensorMetadata: List<MeasurementMetadataEntry>,
    timeWindow: SensorChartTimeWindow,
    onIntent: (MeasurementPreviewIntent) -> Unit,
) {
    when (content) {
        is MeasurementFileContent.SensorSeries -> sensorSeriesItems(content, sensorMetadata, timeWindow, onIntent)
        is MeasurementFileContent.GpsCoordinates -> gpsCoordinateItems(content)
        is MeasurementFileContent.Text -> item { StoredText(content) }
        null -> item { ArchiveError() }
    }
}

private fun LazyListScope.sensorSeriesItems(
    content: MeasurementFileContent.SensorSeries,
    metadata: List<MeasurementMetadataEntry>,
    timeWindow: SensorChartTimeWindow,
    onIntent: (MeasurementPreviewIntent) -> Unit,
) {
    if (metadata.isNotEmpty()) {
        item { SensorBoxSettingsSection(stringResource(R.string.sensor_details)) }
        metadata.forEach { entry -> item { MetadataRow(entry) } }
    }
    item { SensorSeriesSummary(content) }
    item { StoredSensorChart(content.columns, content.samples, timeWindow, onIntent) }
}

private fun LazyListScope.gpsCoordinateItems(content: MeasurementFileContent.GpsCoordinates) {
    item { CoordinateSummary(content) }
    content.coordinates.forEachIndexed { index, coordinate -> item { CoordinateRow(index + 1, coordinate) } }
}
