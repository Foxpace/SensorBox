package com.tomasrepcik.sensorbox.presentation.main

import android.Manifest
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor
import kotlin.math.max

@Composable
fun SensorPreviewScreen(state: RecordingState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val sensor = state.detailsSensorType?.let { type -> state.sensors.firstOrNull { it.type == type } }
    val title = when {
        state.detailsSensorType == null -> stringResource(R.string.gps)
        sensor != null -> sensor.name
        else -> stringResource(R.string.sensor_unavailable)
    }
    SensorBoxBackScreen(title = title, onBack = onBack, modifier = modifier) {
        if (state.detailsSensorType == null) {
            item { GpsPreview(state) }
        } else if (sensor != null) {
            item { HardwareSensorPreview(sensor) }
        }
    }
}

@Composable
private fun HardwareSensorPreview(sensor: SensorDescriptor) {
    val preview = rememberHardwareSensorPreview(sensor.type)
    val latestValues = preview.samples.lastOrNull()?.values
    val unit = sensorUnit(sensor.type)
    Column(Modifier.fillMaxWidth()) {
        if (!preview.isAvailable) {
            Text(stringResource(R.string.sensor_activation_failed), color = MaterialTheme.colorScheme.error)
            return@Column
        }
        if (sensor.type == Sensor.TYPE_STEP_COUNTER) {
            StepCounterPreview(latestValues?.firstOrNull())
        } else {
            SensorValues(latestValues, unit)
            PreviewSectionTitle(stringResource(R.string.live_chart))
            LiveSensorChart(preview.samples, unit)
        }
    }
}

@Composable
private fun StepCounterPreview(value: Float?) {
    if (value == null) {
        Text(stringResource(R.string.waiting_sensor_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value.toLong().toString(), style = MaterialTheme.typography.displayMedium)
        Text(stringResource(R.string.unit_steps), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SensorValues(values: FloatArray?, unit: String) {
    if (values == null) {
        Text(stringResource(R.string.waiting_sensor_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    values.forEachIndexed { index, value ->
        PreviewValueRow(
            label = stringResource(R.string.sensor_value_with_unit, axisLabel(index, values.size), unit),
            value = formatDecimal(value),
            showDivider = index != values.lastIndex,
        )
    }
}

@Composable
private fun PreviewValueRow(label: String, value: String, showDivider: Boolean = true) {
    DetailRow(label, value, Modifier.padding(vertical = 14.dp))
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
    }
}

@Composable
private fun PreviewSectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 12.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun LiveSensorChart(samples: List<TimedSensorSample>, unit: String) {
    val axisCount = samples.maxOfOrNull { it.values.size }?.coerceAtMost(MAX_CHART_AXES) ?: 0
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.outline,
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val validValues = samples.flatMap { sample ->
        sample.values.take(axisCount).filter(Float::isFinite)
    }
    val scale = chartScale(validValues)
    val timeAxisLabel = stringResource(R.string.chart_time_axis)
    val valueAxisLabel = stringResource(R.string.chart_value_axis, unit)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SensorChartCanvas(samples, axisCount, scale, colors, gridColor, axisColor, timeAxisLabel, valueAxisLabel)
        if (axisCount > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(axisCount) { axis -> ChartLegend(axisLabel(axis, axisCount), colors[axis]) }
            }
        }
    }
}

@Composable
private fun SensorChartCanvas(
    samples: List<TimedSensorSample>,
    axisCount: Int,
    scale: ChartScale,
    colors: List<Color>,
    gridColor: Color,
    axisColor: Color,
    timeAxisLabel: String,
    valueAxisLabel: String,
) {
    Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
        val plot = ChartPlot(
            left = CHART_LEFT_MARGIN.toPx(),
            top = CHART_TOP_MARGIN.toPx(),
            right = size.width - CHART_RIGHT_MARGIN.toPx(),
            bottom = size.height - CHART_BOTTOM_MARGIN.toPx(),
        )
        drawChartAxes(plot, scale, samples, gridColor, axisColor, timeAxisLabel, valueAxisLabel)
        if (samples.size < 2) return@Canvas
        drawSensorLines(samples, axisCount, plot, scale, colors)
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
    timeAxisLabel: String,
    valueAxisLabel: String,
) {
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = axisColor.toArgb()
        textSize = 10.sp.toPx()
    }
    val titlePaint = Paint(labelPaint).apply { textSize = 11.sp.toPx() }
    drawValueTicks(plot, scale, gridColor, labelPaint)
    drawTimeTicks(plot, samples, gridColor, labelPaint)
    drawLine(axisColor, Offset(plot.left, plot.top), Offset(plot.left, plot.bottom), 1.5.dp.toPx())
    drawLine(axisColor, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), 1.5.dp.toPx())
    drawAxisTitles(plot, timeAxisLabel, valueAxisLabel, titlePaint)
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

private fun DrawScope.drawAxisTitles(plot: ChartPlot, timeTitle: String, valueTitle: String, paint: Paint) {
    paint.textAlign = Paint.Align.CENTER
    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        nativeCanvas.drawText(timeTitle, plot.left + plot.width / 2f, size.height - 2.dp.toPx(), paint)
        val centerY = plot.top + plot.height / 2f
        val titleX = 10.dp.toPx()
        nativeCanvas.save()
        nativeCanvas.rotate(-90f, titleX, centerY)
        nativeCanvas.drawText(valueTitle, titleX, centerY, paint)
        nativeCanvas.restore()
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
    kotlin.math.abs(value) >= 100f -> ValueFormats.decimal(value, fractionDigits = 0)
    kotlin.math.abs(value) >= 10f -> ValueFormats.decimal(value, fractionDigits = 1)
    else -> ValueFormats.decimal(value)
}

@Composable
private fun ChartLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun GpsPreview(state: RecordingState) {
    var permissionRevision by remember { mutableIntStateOf(0) }
    val permissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionRevision += 1
    }
    val details = rememberGpsDetails(
        intervalSeconds = state.preferences.recording.gpsIntervalSeconds,
        minimumDistanceMeters = state.preferences.recording.gpsMinDistanceMeters,
        permissionRevision = permissionRevision,
    )
    val unavailableValue = stringResource(if (details.hasPermission) R.string.waiting else R.string.unavailable)
    GpsPreviewPanel(details, unavailableValue) {
        permissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
}

@Composable
private fun GpsPreviewPanel(details: GpsDetailsState, unavailableValue: String, onRequestPermission: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        PreviewValueRow(
            stringResource(R.string.location_permission),
            stringResource(if (details.hasPermission) R.string.granted else R.string.required),
        )
        PreviewValueRow(
            stringResource(R.string.detail_latitude),
            details.location?.latitude?.toString() ?: unavailableValue,
        )
        PreviewValueRow(
            stringResource(R.string.detail_longitude),
            details.location?.longitude?.toString() ?: unavailableValue,
        )
        PreviewValueRow(
            stringResource(R.string.detail_altitude),
            details.location?.altitude?.let(::formatDecimal) ?: unavailableValue,
        )
        PreviewValueRow(
            stringResource(R.string.detail_bearing),
            details.location?.bearing?.let(::formatDecimal) ?: unavailableValue,
            showDivider = false,
        )
        if (!details.hasPermission) {
            Text(
                stringResource(R.string.location_permission_explanation),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )
            SensorBoxSecondaryButton(
                label = stringResource(R.string.grant_location_permission),
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun rememberHardwareSensorPreview(sensorType: Int): HardwareSensorPreviewState {
    val context = LocalContext.current
    val sensorManager = remember(context) { context.getSystemService(SensorManager::class.java) }
    val sensor = remember(sensorManager, sensorType) { sensorManager?.getDefaultSensor(sensorType) }
    var samples by remember(sensorType) { mutableStateOf(emptyList<TimedSensorSample>()) }
    var isAvailable by remember(sensorType) { mutableStateOf(sensor != null) }

    DisposableEffect(sensorManager, sensor) {
        if (sensorManager == null || sensor == null) {
            isAvailable = false
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val sample = TimedSensorSample(event.timestamp, event.values.copyOf())
                    samples = (samples + sample).takeLast(MAX_CHART_SAMPLES)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            isAvailable = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }
    return HardwareSensorPreviewState(isAvailable, samples)
}

@Composable
private fun axisLabel(index: Int, axisCount: Int): String = when {
    axisCount == 1 -> stringResource(R.string.value)
    index == 0 -> stringResource(R.string.axis_x)
    index == 1 -> stringResource(R.string.axis_y)
    index == 2 -> stringResource(R.string.axis_z)
    index == 3 -> stringResource(R.string.axis_w)
    else -> stringResource(R.string.value_number, index + 1)
}

private data class HardwareSensorPreviewState(val isAvailable: Boolean, val samples: List<TimedSensorSample>)

private data class TimedSensorSample(val timestampNanos: Long, val values: FloatArray)

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
private const val MAX_CHART_SAMPLES = 90
private const val MAX_CHART_AXES = 6
private const val GRID_LINE_COUNT = 5
private const val TIME_TICK_COUNT = 3
private const val MIN_CHART_RANGE = 0.001f
private const val CHART_RANGE_PADDING = 0.08f
private const val NANOS_PER_SECOND = 1_000_000_000f
