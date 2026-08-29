package com.tomasrepcik.sensorbox.measurements.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxSettingsDivider
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileKind
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileSummary
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementMetadataEntry

@Composable
internal fun MeasurementLoading() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(stringResource(R.string.loading_measurements))
    }
}

@Composable
internal fun ArchiveError() {
    Text(
        stringResource(R.string.measurements_read_failed),
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
internal fun EmptyArchive() {
    Text(
        stringResource(R.string.measurements_empty),
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun MetadataRow(entry: MeasurementMetadataEntry) {
    DetailValueRow(metadataLabel(entry.name), entry.value)
}

@Composable
internal fun DetailValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, Modifier.weight(0.42f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(0.58f))
    }
    SensorBoxSettingsDivider()
}

@Composable
internal fun MeasurementFileRow(file: MeasurementFileSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = when (file.kind) {
                    MeasurementFileKind.SENSOR -> stringResource(R.string.sensor_samples)
                    MeasurementFileKind.GPS -> stringResource(R.string.gps_coordinates)
                    MeasurementFileKind.TEXT -> stringResource(R.string.text_data)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatBytes(file.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    SensorBoxSettingsDivider()
}

private fun metadataLabel(value: String): String = value.replace(CAMEL_CASE_BOUNDARY, " ")
    .replace('.', ' ')
    .replace('_', ' ')
    .replaceFirstChar(Char::titlecase)

private fun formatBytes(value: Long): String = when {
    value >= BYTES_PER_MEGABYTE -> "${value / BYTES_PER_MEGABYTE} MB"
    value >= BYTES_PER_KILOBYTE -> "${value / BYTES_PER_KILOBYTE} KB"
    else -> "$value B"
}

private const val BYTES_PER_KILOBYTE = 1_024L
private const val BYTES_PER_MEGABYTE = BYTES_PER_KILOBYTE * 1_024L
private val CAMEL_CASE_BOUNDARY = Regex("(?<=[a-z0-9])(?=[A-Z])")
