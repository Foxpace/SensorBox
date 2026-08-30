package com.tomasrepcik.sensorbox.measurements.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxSettingsDivider
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementSummary

internal fun LazyListScope.measurementArchiveItems(
    state: MeasurementBrowserState,
    onIntent: (MeasurementBrowserIntent) -> Unit,
) {
    when {
        state.isLoading && state.measurements.isEmpty() -> item { MeasurementLoading() }

        state.errorCode != null -> item { ArchiveError() }

        state.measurements.isEmpty() -> item { EmptyArchive() }

        else -> state.measurements.forEach { measurement ->
            item {
                MeasurementRow(measurement) {
                    onIntent(MeasurementBrowserIntent.OpenMeasurementDetails(measurement.id))
                }
            }
        }
    }
    if (state.isLoading && state.measurements.isNotEmpty()) item { MeasurementLoading() }
}

@Composable
private fun MeasurementRow(measurement: MeasurementSummary, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            measurement.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        measurement.recordedAtText?.let { recordedAt ->
            Text(recordedAt, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            pluralStringResource(
                R.plurals.measurement_file_count,
                measurement.fileCount,
                measurement.fileCount,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    SensorBoxSettingsDivider()
}
