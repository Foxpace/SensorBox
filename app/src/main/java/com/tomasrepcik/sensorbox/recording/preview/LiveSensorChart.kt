package com.tomasrepcik.sensorbox.recording.preview

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withRotation
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import kotlin.math.abs
import kotlin.math.max

@Composable
internal fun LiveSensorChart(samples: List<TimedSensorSample>, unit: String) {
    val axisCount = samples.maxOfOrNull { it.values.size }?.coerceAtMost(MAX_CHART_AXES) ?: 0
    val colors = liveChartColors()
    val values = samples.flatMap { sample -> sample.values.take(axisCount).filter(Float::isFinite) }
    val labels = LiveChartLabels(
        timeAxis = stringResource(R.string.chart_time_axis),
        valueAxis = stringResource(R.string.chart_value_axis, unit),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LiveSensorChartCanvas(samples, axisCount, chartScale(values), colors, labels)
        if (axisCount > 0) LiveSensorChartLegend(axisCount, colors)
    }
}

@Composable
private fun liveChartColors(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.error,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.onSurface,
    MaterialTheme.colorScheme.outline,
)

@Composable
private fun LiveSensorChartCanvas(
    samples: List<TimedSensorSample>,
    axisCount: Int,
    scale: ChartScale,
    colors: List<Color>,
    labels: LiveChartLabels,
) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
        val plot = ChartPlot(
            left = CHART_LEFT_MARGIN.toPx(),
            top = CHART_TOP_MARGIN.toPx(),
            right = size.width - CHART_RIGHT_MARGIN.toPx(),
            bottom = size.height - CHART_BOTTOM_MARGIN.toPx(),
        )
        drawChartAxes(plot, scale, samples, gridColor, axisColor, labels)
        if (samples.size >= 2) drawSensorLines(samples, axisCount, plot, scale, colors)
    }
}

@Composable
private fun LiveSensorChartLegend(axisCount: Int, colors: List<Color>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        repeat(axisCount) { axis -> ChartLegend(axisLabel(axis, axisCount), colors[axis]) }
    }
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun chartScale(values: List<Float>): ChartScale {
    if (values.isEmpty()) return ChartScale(-1f, 1f)
    val rawMinimum = values.minOrNull() ?: -1f
    val rawMaximum = values.maxOrNull() ?: 1f
    val padding = max((rawMaximum - rawMinimum) * CHART_RANGE_PADDING, MIN_CHART_RANGE / 2f)
    return ChartScale(rawMinimum - padding, rawMaximum + padding)
}

private fun DrawScope.drawChartAxes(
    plot: ChartPlot,
    scale: ChartScale,
    samples: List<TimedSensorSample>,
    gridColor: Color,
    axisColor: Color,
    labels: LiveChartLabels,
) {
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = axisColor.toArgb()
        textSize = 10.sp.toPx()
    }
    drawValueTicks(plot, scale, gridColor, labelPaint)
    drawTimeTicks(plot, samples, gridColor, labelPaint)
    drawLine(axisColor, Offset(plot.left, plot.top), Offset(plot.left, plot.bottom), 1.5.dp.toPx())
    drawLine(axisColor, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), 1.5.dp.toPx())
    drawAxisTitles(plot, labels, Paint(labelPaint).apply { textSize = 11.sp.toPx() })
}

private fun DrawScope.drawValueTicks(plot: ChartPlot, scale: ChartScale, gridColor: Color, paint: Paint) {
    paint.textAlign = Paint.Align.RIGHT
    repeat(GRID_LINE_COUNT) { index ->
        val fraction = index.toFloat() / (GRID_LINE_COUNT - 1)
        val y = plot.bottom - plot.height * fraction
        val value = scale.minimum + scale.range * fraction
        drawLine(gridColor, Offset(plot.left, y), Offset(plot.right, y), 1.dp.toPx())
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(formatChartValue(value), plot.left - 7.dp.toPx(), y + 3.dp.toPx(), paint)
        }
    }
    if (scale.minimum < 0f && scale.maximum > 0f) {
        val zeroY = plot.bottom - plot.height * (-scale.minimum / scale.range)
        drawLine(gridColor, Offset(plot.left, zeroY), Offset(plot.right, zeroY), 2.dp.toPx())
    }
}

private fun DrawScope.drawTimeTicks(
    plot: ChartPlot,
    samples: List<TimedSensorSample>,
    gridColor: Color,
    paint: Paint,
) {
    paint.textAlign = Paint.Align.CENTER
    val durationSeconds = samples.durationSeconds()
    repeat(TIME_TICK_COUNT) { index ->
        val fraction = index.toFloat() / (TIME_TICK_COUNT - 1)
        val x = plot.left + plot.width * fraction
        drawLine(gridColor, Offset(x, plot.top), Offset(x, plot.bottom), 1.dp.toPx())
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                formatChartTime(durationSeconds * fraction),
                x,
                plot.bottom + 14.dp.toPx(),
                paint,
            )
        }
    }
}

private fun DrawScope.drawAxisTitles(plot: ChartPlot, labels: LiveChartLabels, paint: Paint) {
    paint.textAlign = Paint.Align.CENTER
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        nativeCanvas.drawText(labels.timeAxis, plot.left + plot.width / 2f, size.height - 2.dp.toPx(), paint)
        val centerY = plot.top + plot.height / 2f
        val titleX = 10.dp.toPx()
        nativeCanvas.withRotation(-90f, titleX, centerY) {
            drawText(labels.valueAxis, titleX, centerY, paint)
        }
    }
}

private fun DrawScope.drawSensorLines(
    samples: List<TimedSensorSample>,
    axisCount: Int,
    plot: ChartPlot,
    scale: ChartScale,
    colors: List<Color>,
) {
    val firstTimestamp = samples.first().timestampNanos
    val durationNanos = (samples.last().timestampNanos - firstTimestamp).coerceAtLeast(1L)
    repeat(axisCount) { axis ->
        val path = Path()
        var hasPoint = false
        samples.forEach { sample ->
            val value = sample.values.getOrNull(axis)?.takeIf(Float::isFinite) ?: return@forEach
            val elapsedFraction = (sample.timestampNanos - firstTimestamp).toFloat() / durationNanos
            val x = plot.left + plot.width * elapsedFraction
            val y = plot.bottom - plot.height * ((value - scale.minimum) / scale.range)
            if (hasPoint) path.lineTo(x, y) else path.moveTo(x, y)
            hasPoint = true
        }
        drawPath(path, colors[axis], style = Stroke(width = 2.5.dp.toPx()))
    }
}

private fun List<TimedSensorSample>.durationSeconds(): Float =
    if (size < 2) 0f else (last().timestampNanos - first().timestampNanos) / NANOS_PER_SECOND

private fun formatChartTime(seconds: Float): String = ValueFormats.decimal(seconds, fractionDigits = 1)

private fun formatChartValue(value: Float): String = when {
    abs(value) >= 100f -> ValueFormats.decimal(value, fractionDigits = 0)
    abs(value) >= 10f -> ValueFormats.decimal(value, fractionDigits = 1)
    else -> ValueFormats.decimal(value)
}

private data class LiveChartLabels(val timeAxis: String, val valueAxis: String)

private data class ChartScale(val minimum: Float, val maximum: Float) {
    val range: Float = maximum - minimum
}

private data class ChartPlot(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float = right - left
    val height: Float = bottom - top
}

private val CHART_HEIGHT = 260.dp
private val CHART_LEFT_MARGIN = 56.dp
private val CHART_RIGHT_MARGIN = 10.dp
private val CHART_TOP_MARGIN = 10.dp
private val CHART_BOTTOM_MARGIN = 42.dp
private const val MAX_CHART_AXES = 6
private const val GRID_LINE_COUNT = 5
private const val TIME_TICK_COUNT = 3
private const val MIN_CHART_RANGE = 0.001f
private const val CHART_RANGE_PADDING = 0.08f
private const val NANOS_PER_SECOND = 1_000_000_000f
