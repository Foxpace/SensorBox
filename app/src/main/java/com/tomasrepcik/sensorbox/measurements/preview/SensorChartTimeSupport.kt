package com.tomasrepcik.sensorbox.measurements.preview

import com.tomasrepcik.sensorbox.measurements.storage.SensorSeriesSample

internal fun selectSensorSamplesInTimeWindow(
    samples: List<SensorSeriesSample>,
    timeWindow: SensorChartTimeWindow,
): List<SensorSeriesSample> {
    if (samples.size <= 2) return samples
    val firstTimestamp = samples.first().timestampMillis
    val duration = samples.last().timestampMillis - firstTimestamp
    if (duration <= 0L) return samples
    val startTimestamp = firstTimestamp + (duration * timeWindow.startFraction).toLong()
    val endTimestamp = firstTimestamp + (duration * timeWindow.endFraction).toLong()
    val firstVisibleIndex = samples.indexOfFirst { it.timestampMillis >= startTimestamp }
        .let { if (it < 0) samples.lastIndex else it }
    val lastVisibleIndex = samples.indexOfLast { it.timestampMillis <= endTimestamp }
        .let { if (it < 0) 0 else it }
    val startIndex = (firstVisibleIndex - 1).coerceAtLeast(0)
    val endIndex = (lastVisibleIndex + 1).coerceAtMost(samples.lastIndex).coerceAtLeast(startIndex)
    return samples.subList(startIndex, endIndex + 1)
}

internal fun formatSensorChartDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / MILLIS_PER_SECOND
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return if (minutes > 0) "$minutes min $seconds s" else "$seconds s"
}

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
