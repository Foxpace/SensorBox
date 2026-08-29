package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementDetails

internal fun LazyListScope.measurementDetailsItems(
    details: MeasurementDetails,
    state: MeasurementBrowserState,
    onIntent: (MeasurementBrowserIntent) -> Unit,
) {
    item { SensorBoxSettingsSection(stringResource(R.string.measurement_summary)) }
    item { MeasurementSummaryRows(details) }
    item { SensorBoxSettingsSection(stringResource(R.string.measurement_metadata)) }
    if (details.metadata.isEmpty()) {
        item { Text(stringResource(R.string.measurement_metadata_empty), Modifier.padding(vertical = 16.dp)) }
    } else {
        details.metadata.forEach { entry -> item { MetadataRow(entry) } }
    }
    item { SensorBoxSettingsSection(stringResource(R.string.measurement_files)) }
    if (details.files.isEmpty()) {
        item { Text(stringResource(R.string.measurement_files_empty), Modifier.padding(vertical = 16.dp)) }
    } else {
        details.files.forEach { file ->
            item { MeasurementFileRow(file) { onIntent(MeasurementBrowserIntent.OpenMeasurementFile(file.id)) } }
        }
    }
    if (state.isLoading) item { MeasurementLoading() }
    if (state.errorCode != null) item { ArchiveError() }
}

@Composable
private fun MeasurementSummaryRows(details: MeasurementDetails) {
    Column {
        DetailValueRow(stringResource(R.string.measurement_folder), details.summary.name)
        details.summary.recordedAtText?.let { DetailValueRow(stringResource(R.string.measurement_started), it) }
        DetailValueRow(stringResource(R.string.measurement_files), details.summary.fileCount.toString())
    }
}
