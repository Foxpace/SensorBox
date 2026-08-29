package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileContent
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementMetadataEntry

@Composable
fun MeasurementFileScreen(
    state: MeasurementBrowserState,
    onIntent: (MeasurementBrowserIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val file = state.selectedFile
    val sensorMetadata = file?.let { selectedFile ->
        state.selectedMeasurement?.sensorMetadataByFile?.get(selectedFile.id)
    }.orEmpty()
    SensorBoxBackScreen(
        title = file?.name ?: stringResource(R.string.measurement_file),
        onBack = onBack,
        modifier = modifier,
    ) {
        measurementFileItems(state.selectedFileContent, sensorMetadata, state.sensorChartTimeWindow, onIntent)
    }
}

private fun LazyListScope.measurementFileItems(
    content: MeasurementFileContent?,
    sensorMetadata: List<MeasurementMetadataEntry>,
    timeWindow: SensorChartTimeWindow,
    onIntent: (MeasurementBrowserIntent) -> Unit,
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
    onIntent: (MeasurementBrowserIntent) -> Unit,
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
