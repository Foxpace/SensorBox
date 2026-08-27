package com.tomasrepcik.sensorbox.presentation.main

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import com.tomasrepcik.sensorbox.domain.measurements.GpsCoordinate
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementDetails
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileContent
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileKind
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileSummary
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementMetadataEntry
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementSummary
import com.tomasrepcik.sensorbox.domain.measurements.SensorSeriesSample
import java.text.DateFormat
import java.util.Date
import kotlin.math.max

@Composable
fun MeasurementsScreen(
    state: MeasurementBrowserState,
    onIntent: (MeasurementBrowserIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onIntent(MeasurementBrowserIntent.RefreshMeasurements) }
    SensorBoxBackScreen(
        title = stringResource(R.string.measurements),
        onBack = onBack,
        modifier = modifier,
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
}

@Composable
fun MeasurementDetailsScreen(
    state: MeasurementBrowserState,
    onIntent: (MeasurementBrowserIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = state.selectedMeasurement
    SensorBoxBackScreen(
        title = stringResource(R.string.measurement_details),
        onBack = onBack,
        modifier = modifier,
        itemSpacing = 0.dp,
    ) {
        if (details == null) {
            item { ArchiveError() }
        } else {
            measurementDetailsItems(details, state, onIntent)
        }
    }
}

private fun LazyListScope.measurementDetailsItems(
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
fun MeasurementFileScreen(
    state: MeasurementBrowserState,
    onIntent: (MeasurementBrowserIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val file = state.selectedFile
    val content = state.selectedFileContent
    SensorBoxBackScreen(
        title = file?.name ?: stringResource(R.string.measurement_file),
        onBack = onBack,
        modifier = modifier,
    ) {
        when (content) {
            is MeasurementFileContent.SensorSeries -> {
                item { SensorSeriesSummary(content) }
                item {
                    StoredSensorChart(
                        columns = content.columns,
                        samples = content.samples,
                        timeWindow = state.sensorChartTimeWindow,
                        onIntent = onIntent,
                    )
                }
            }

            is MeasurementFileContent.GpsCoordinates -> {
                item { CoordinateSummary(content) }
                content.coordinates.forEachIndexed { index, coordinate ->
                    item { CoordinateRow(index + 1, coordinate) }
                }
            }

            is MeasurementFileContent.Text -> item { StoredText(content) }

            null -> item { ArchiveError() }
        }
    }
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
            stringResource(R.string.measurement_file_count, measurement.fileCount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    SensorBoxSettingsDivider()
}

@Composable
private fun MeasurementSummaryRows(details: MeasurementDetails) {
    Column {
        DetailValueRow(stringResource(R.string.measurement_folder), details.summary.name)
        details.summary.recordedAtText?.let { DetailValueRow(stringResource(R.string.measurement_started), it) }
        DetailValueRow(
            stringResource(R.string.measurement_files),
            details.summary.fileCount.toString(),
        )
    }
}

@Composable
private fun MetadataRow(entry: MeasurementMetadataEntry) {
    DetailValueRow(metadataLabel(entry.name), entry.value)
}

@Composable
private fun DetailValueRow(label: String, value: String) {
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
private fun MeasurementFileRow(file: MeasurementFileSummary, onClick: () -> Unit) {
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

@Composable
private fun SensorSeriesSummary(content: MeasurementFileContent.SensorSeries) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            stringResource(R.string.chart_sample_count, content.samples.size),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(content.columns.joinToString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (content.truncated) {
            Text(
                stringResource(R.string.chart_sampled_notice),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StoredSensorChart(
    columns: List<String>,
    samples: List<SensorSeriesSample>,
    timeWindow: SensorChartTimeWindow,
    onIntent: (MeasurementBrowserIntent) -> Unit,
) {
    if (samples.isEmpty() || columns.isEmpty()) {
        Text(stringResource(R.string.no_samples), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.secondary,
    )
    val orderedSamples = remember(samples) { samples.sortedBy(SensorSeriesSample::timestampMillis) }
    val visibleSamples = remember(orderedSamples, timeWindow) {
        selectSensorSamplesInTimeWindow(orderedSamples, timeWindow)
    }
    val fullDurationMillis = (orderedSamples.last().timestampMillis - orderedSamples.first().timestampMillis)
        .coerceAtLeast(0L)
    val visibleDurationMillis = (fullDurationMillis * timeWindow.visibleFraction).toLong().coerceAtLeast(0L)
    val visibleStartTimestamp = orderedSamples.first().timestampMillis +
        (fullDurationMillis * timeWindow.startFraction).toLong()
    val visibleEndTimestamp = orderedSamples.first().timestampMillis +
        (fullDurationMillis * timeWindow.endFraction).toLong()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (zoom != 1f) {
                            onIntent(
                                MeasurementBrowserIntent.ZoomSensorChartTimeWindow(
                                    zoomFactor = zoom,
                                    focalPointFraction = centroid.x / size.width.coerceAtLeast(1).toFloat(),
                                ),
                            )
                        }
                        if (pan.x != 0f) {
                            onIntent(
                                MeasurementBrowserIntent.ShiftSensorChartTimeWindow(
                                    visibleWindowFraction = -pan.x / size.width.coerceAtLeast(1).toFloat(),
                                ),
                            )
                        }
                    }
                },
        ) {
            drawStoredChart(
                samples = visibleSamples,
                columnCount = columns.size,
                colors = colors,
                visibleStartTimestamp = visibleStartTimestamp,
                visibleEndTimestamp = visibleEndTimestamp,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(
                onClick = {
                    onIntent(MeasurementBrowserIntent.ZoomSensorChartTimeWindow(0.5f, 0.5f))
                },
            ) { Text(stringResource(R.string.sensor_chart_zoom_out)) }
            TextButton(
                onClick = { onIntent(MeasurementBrowserIntent.ShowEntireSensorChartTimeRange) },
            ) { Text(stringResource(R.string.sensor_chart_show_all)) }
            TextButton(
                onClick = {
                    onIntent(MeasurementBrowserIntent.ZoomSensorChartTimeWindow(2f, 0.5f))
                },
            ) { Text(stringResource(R.string.sensor_chart_zoom_in)) }
        }
        Text(
            stringResource(
                R.string.sensor_chart_visible_duration,
                formatSensorChartDuration(visibleDurationMillis),
                formatSensorChartDuration(fullDurationMillis),
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(R.string.sensor_chart_gesture_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            columns.forEachIndexed { index, column ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.size(10.dp).background(colors[index % colors.size]))
                    Spacer(Modifier.size(4.dp))
                    Text(column, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStoredChart(
    samples: List<SensorSeriesSample>,
    columnCount: Int,
    colors: List<Color>,
    visibleStartTimestamp: Long,
    visibleEndTimestamp: Long,
) {
    val values = samples.flatMap(SensorSeriesSample::values)
    val minimum = values.minOrNull() ?: 0.0
    val maximum = values.maxOrNull() ?: 1.0
    val range = max(maximum - minimum, MINIMUM_CHART_RANGE)
    val left = CHART_LABEL_WIDTH
    val widthAvailable = size.width - left
    val heightAvailable = size.height - CHART_BOTTOM_PADDING
    repeat(CHART_GRID_LINES + 1) { line ->
        val y = heightAvailable * line / CHART_GRID_LINES
        drawLine(Color.Gray.copy(alpha = 0.25f), Offset(left, y), Offset(size.width, y))
        val value = maximum - range * line / CHART_GRID_LINES
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = Color.Gray.toArgb()
                textSize = 10.sp.toPx()
            }
            canvas.nativeCanvas.drawText(ValueFormats.decimal(value.toFloat()), 4.dp.toPx(), y + 4.dp.toPx(), paint)
        }
    }
    val timestampRange = (visibleEndTimestamp - visibleStartTimestamp).coerceAtLeast(1L)
    clipRect(left = left, top = 0f, right = size.width, bottom = heightAvailable) {
        repeat(columnCount) { column ->
            val path = Path()
            var hasPoint = false
            samples.forEach { sample ->
                val value = sample.values.getOrNull(column) ?: return@forEach
                val elapsedFraction = (sample.timestampMillis - visibleStartTimestamp).toFloat() / timestampRange
                val x = left + widthAvailable * elapsedFraction
                val y = ((maximum - value) / range * heightAvailable).toFloat()
                if (!hasPoint) {
                    path.moveTo(x, y)
                    hasPoint = true
                } else {
                    path.lineTo(x, y)
                }
            }
            drawPath(path, colors[column % colors.size], style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
private fun CoordinateSummary(content: MeasurementFileContent.GpsCoordinates) {
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
private fun CoordinateRow(index: Int, coordinate: GpsCoordinate) {
    SensorBoxPanel(Modifier.padding(vertical = 5.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.coordinate_number, index), style = MaterialTheme.typography.titleSmall)
            Text(
                "${ValueFormats.decimal(coordinate.latitude, GPS_FRACTION_DIGITS)} , " +
                    ValueFormats.decimal(coordinate.longitude, GPS_FRACTION_DIGITS),
            )
            Text(
                formatTimestamp(coordinate.timestampMillis),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
private fun StoredText(content: MeasurementFileContent.Text) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (content.truncated) {
            Text(
                stringResource(R.string.text_truncated_notice),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(content.value, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun MeasurementLoading() {
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
private fun ArchiveError() {
    Text(
        stringResource(R.string.measurements_read_failed),
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun EmptyArchive() {
    Text(
        stringResource(R.string.measurements_empty),
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun metadataLabel(value: String): String = value.replace('.', ' ').replace('_', ' ')
    .replaceFirstChar(Char::titlecase)

private fun formatBytes(value: Long): String = when {
    value >= BYTES_PER_MEGABYTE -> "${value / BYTES_PER_MEGABYTE} MB"
    value >= BYTES_PER_KILOBYTE -> "${value / BYTES_PER_KILOBYTE} KB"
    else -> "$value B"
}

private fun formatTimestamp(value: Long): String = DateFormat.getDateTimeInstance().format(Date(value))

private const val BYTES_PER_KILOBYTE = 1_024L
private const val BYTES_PER_MEGABYTE = BYTES_PER_KILOBYTE * 1_024L
private const val MINIMUM_CHART_RANGE = 0.0001
private const val CHART_LABEL_WIDTH = 52f
private const val CHART_BOTTOM_PADDING = 12f
private const val CHART_GRID_LINES = 4
private const val GPS_FRACTION_DIGITS = 6
