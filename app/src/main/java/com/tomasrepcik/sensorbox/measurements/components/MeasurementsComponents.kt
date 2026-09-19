package com.tomasrepcik.sensorbox.measurements.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.measurements.list.MeasurementsIntent
import com.tomasrepcik.sensorbox.measurements.list.MeasurementsState
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementSummary

internal fun LazyListScope.measurementArchiveItems(state: MeasurementsState, onIntent: (MeasurementsIntent) -> Unit) {
    when {
        state.isLoading && state.measurements.isEmpty() -> item {
            MeasurementLoading(Modifier.fillParentMaxHeight())
        }

        state.errorCode != null -> item { ArchiveError() }

        state.measurements.isEmpty() -> item { EmptyArchive() }

        else -> state.recordings.forEach { recording ->
            item {
                RecordingCard(recording, onIntent)
            }
        }
    }
    if (state.isLoading && state.measurements.isNotEmpty()) item { MeasurementLoading() }
}

@Composable
private fun RecordingCard(recording: List<MeasurementSummary>, onIntent: (MeasurementsIntent) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                recording.first().recordingName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (recording.any { it.device == "phone" } && recording.any { it.device == "watch" }) {
                Text(
                    stringResource(R.string.measurement_pair),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        recording.forEach { measurement ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MeasurementRow(measurement) { onIntent(MeasurementsIntent.OpenDetails(measurement.id)) }
        }
    }
}

@Composable
private fun MeasurementRow(measurement: MeasurementSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(
                when (measurement.device) {
                    "phone" -> R.drawable.ic_measurement_phone
                    "watch" -> R.drawable.ic_sync_watch
                    else -> R.drawable.ic_baseline_folder
                },
            ),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                when (measurement.device) {
                    "phone" -> stringResource(R.string.measurement_phone)
                    "watch" -> stringResource(R.string.measurement_watch)
                    else -> measurement.name
                },
                style = MaterialTheme.typography.titleSmall,
            )
            measurement.recordedAtText?.let { recordedAt ->
                Text(
                    recordedAt,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                pluralStringResource(R.plurals.measurement_file_count, measurement.fileCount, measurement.fileCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(
            painterResource(R.drawable.ic_measurement_open),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
