package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import com.tomasrepcik.sensorbox.domain.measurements.GpsCoordinate
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileContent
import java.text.DateFormat
import java.util.Date

@Composable
internal fun SensorSeriesSummary(content: MeasurementFileContent.SensorSeries) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(R.string.chart_sample_count, content.samples.size),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(content.columns.joinToString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (content.truncated) {
            Text(stringResource(R.string.chart_sampled_notice), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun CoordinateSummary(content: MeasurementFileContent.GpsCoordinates) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(R.string.coordinate_count, content.coordinates.size),
            style = MaterialTheme.typography.titleMedium,
        )
        if (content.truncated) {
            Text(
                stringResource(R.string.coordinates_truncated_notice),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun CoordinateRow(index: Int, coordinate: GpsCoordinate) {
    SensorBoxPanel(Modifier.padding(vertical = 5.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.coordinate_number, index), style = MaterialTheme.typography.titleSmall)
            Text(
                "${ValueFormats.decimal(coordinate.latitude, GPS_FRACTION_DIGITS)} , " +
                    ValueFormats.decimal(coordinate.longitude, GPS_FRACTION_DIGITS),
            )
            Text(formatTimestamp(coordinate.timestampMillis), color = MaterialTheme.colorScheme.onSurfaceVariant)
            coordinate.altitude?.let {
                Text(
                    stringResource(R.string.coordinate_altitude, ValueFormats.decimal(it)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            coordinate.accuracyMeters?.let {
                Text(
                    stringResource(R.string.coordinate_accuracy, ValueFormats.decimal(it)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun StoredText(content: MeasurementFileContent.Text) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (content.truncated) {
            Text(stringResource(R.string.text_truncated_notice), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(content.value, fontFamily = FontFamily.Monospace)
    }
}

private fun formatTimestamp(value: Long): String = DateFormat.getDateTimeInstance().format(Date(value))

private const val GPS_FRACTION_DIGITS = 6
