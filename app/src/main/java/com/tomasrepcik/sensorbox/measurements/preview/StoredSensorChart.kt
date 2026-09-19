package com.tomasrepcik.sensorbox.measurements.preview

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import com.tomasrepcik.sensorbox.measurements.storage.SensorSeriesSample
import kotlin.math.max

@Composable
internal fun StoredSensorChart(
    columns: List<String>,
    samples: List<SensorSeriesSample>,
    timeWindow: SensorChartTimeWindow,
    onIntent: (MeasurementPreviewIntent) -> Unit,
) {
    if (samples.isEmpty() || columns.isEmpty()) {
        Text(stringResource(R.string.no_samples), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val colors = storedChartColors()
    val viewport = rememberStoredChartViewport(samples, timeWindow)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StoredChartPlot(viewport, columns.size, colors, onIntent)
        StoredChartControls(onIntent)
        StoredChartTimeSummary(viewport)
        StoredChartLegend(columns, colors)
    }
}

@Composable
private fun storedChartColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.error,
    Color(0xFF00897B),
    Color(0xFFFB8C00),
    Color(0xFF8E24AA),
    Color(0xFF039BE5),
    Color(0xFF7CB342),
)

@Composable
private fun rememberStoredChartViewport(
    samples: List<SensorSeriesSample>,
    timeWindow: SensorChartTimeWindow,
): StoredChartViewport {
    val orderedSamples = remember(samples) { samples.sortedBy(SensorSeriesSample::timestampMillis) }
    val visibleSamples = remember(orderedSamples, timeWindow) {
        selectSensorSamplesInTimeWindow(orderedSamples, timeWindow)
    }
    val firstTimestamp = orderedSamples.first().timestampMillis
    val fullDuration = (orderedSamples.last().timestampMillis - firstTimestamp).coerceAtLeast(0L)
    return StoredChartViewport(
        samples = visibleSamples,
        fullDurationMillis = fullDuration,
        visibleDurationMillis = (fullDuration * timeWindow.visibleFraction).toLong().coerceAtLeast(0L),
        visibleStartTimestamp = firstTimestamp + (fullDuration * timeWindow.startFraction).toLong(),
        visibleEndTimestamp = firstTimestamp + (fullDuration * timeWindow.endFraction).toLong(),
    )
}

@Composable
private fun StoredChartPlot(
    viewport: StoredChartViewport,
    columnCount: Int,
    colors: List<Color>,
    onIntent: (MeasurementPreviewIntent) -> Unit,
) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val chartWidth = size.width.coerceAtLeast(1).toFloat()
                    if (zoom != 1f) {
                        onIntent(
                            MeasurementPreviewIntent.ZoomSensorChartTimeWindow(
                                zoomFactor = zoom,
                                focalPointFraction = centroid.x / chartWidth,
                            ),
                        )
                    }
                    if (pan.x != 0f) {
                        onIntent(
                            MeasurementPreviewIntent.ShiftSensorChartTimeWindow(
                                visibleWindowFraction = -pan.x / chartWidth,
                            ),
                        )
                    }
                }
            },
    ) {
        drawStoredChart(viewport, columnCount, colors)
    }
}

@Composable
private fun StoredChartControls(onIntent: (MeasurementPreviewIntent) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { onIntent(MeasurementPreviewIntent.ZoomSensorChartTimeWindow(0.5f, 0.5f)) }) {
            Text(stringResource(R.string.sensor_chart_zoom_out))
        }
        TextButton(onClick = { onIntent(MeasurementPreviewIntent.ShowEntireSensorChartTimeRange) }) {
            Text(stringResource(R.string.sensor_chart_show_all))
        }
        TextButton(onClick = { onIntent(MeasurementPreviewIntent.ZoomSensorChartTimeWindow(2f, 0.5f)) }) {
            Text(stringResource(R.string.sensor_chart_zoom_in))
        }
    }
}

@Composable
private fun StoredChartTimeSummary(viewport: StoredChartViewport) {
    Text(
        stringResource(
            R.string.sensor_chart_visible_duration,
            formatSensorChartDuration(viewport.visibleDurationMillis),
            formatSensorChartDuration(viewport.fullDurationMillis),
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        stringResource(R.string.sensor_chart_gesture_hint),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun StoredChartLegend(columns: List<String>, colors: List<Color>) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        columns.forEachIndexed { index, column ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(10.dp).background(colors[index % colors.size]))
                Spacer(Modifier.size(4.dp))
                val label = if (isActivityColumn(column)) stringResource(activityNameResource(column)) else column
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun DrawScope.drawStoredChart(viewport: StoredChartViewport, columnCount: Int, colors: List<Color>) {
    val values = viewport.samples.flatMap(SensorSeriesSample::values)
    val minimum = values.minOrNull() ?: 0.0
    val maximum = values.maxOrNull() ?: 1.0
    val range = max(maximum - minimum, MINIMUM_CHART_RANGE)
    val widthAvailable = size.width - CHART_LABEL_WIDTH
    val heightAvailable = size.height - CHART_BOTTOM_PADDING
    drawStoredChartGrid(maximum, range, heightAvailable)
    drawStoredChartLines(
        viewport = viewport,
        columnCount = columnCount,
        colors = colors,
        maximum = maximum,
        range = range,
        widthAvailable = widthAvailable,
        heightAvailable = heightAvailable,
    )
}

private fun DrawScope.drawStoredChartGrid(maximum: Double, range: Double, heightAvailable: Float) {
    repeat(CHART_GRID_LINES + 1) { line ->
        val y = heightAvailable * line / CHART_GRID_LINES
        drawLine(Color.Gray.copy(alpha = 0.25f), Offset(CHART_LABEL_WIDTH, y), Offset(size.width, y))
        val value = maximum - range * line / CHART_GRID_LINES
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = Color.Gray.toArgb()
                textSize = 10.sp.toPx()
            }
            canvas.nativeCanvas.drawText(ValueFormats.decimal(value.toFloat()), 4.dp.toPx(), y + 4.dp.toPx(), paint)
        }
    }
}

private fun DrawScope.drawStoredChartLines(
    viewport: StoredChartViewport,
    columnCount: Int,
    colors: List<Color>,
    maximum: Double,
    range: Double,
    widthAvailable: Float,
    heightAvailable: Float,
) {
    val timestampRange = (viewport.visibleEndTimestamp - viewport.visibleStartTimestamp).coerceAtLeast(1L)
    clipRect(left = CHART_LABEL_WIDTH, top = 0f, right = size.width, bottom = heightAvailable) {
        repeat(columnCount) { column ->
            val path = Path()
            var hasPoint = false
            viewport.samples.forEach { sample ->
                val value = sample.values.getOrNull(column) ?: return@forEach
                val elapsed = (sample.timestampMillis - viewport.visibleStartTimestamp).toFloat() / timestampRange
                val x = CHART_LABEL_WIDTH + widthAvailable * elapsed
                val y = ((maximum - value) / range * heightAvailable).toFloat()
                if (hasPoint) path.lineTo(x, y) else path.moveTo(x, y)
                hasPoint = true
            }
            drawPath(path, colors[column % colors.size], style = Stroke(width = 2.dp.toPx()))
        }
    }
}

private data class StoredChartViewport(
    val samples: List<SensorSeriesSample>,
    val fullDurationMillis: Long,
    val visibleDurationMillis: Long,
    val visibleStartTimestamp: Long,
    val visibleEndTimestamp: Long,
)

private const val MINIMUM_CHART_RANGE = 0.0001
private const val CHART_LABEL_WIDTH = 52f
private const val CHART_BOTTOM_PADDING = 12f
private const val CHART_GRID_LINES = 4
