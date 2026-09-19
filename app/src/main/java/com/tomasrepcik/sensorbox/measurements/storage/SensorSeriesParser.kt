package com.tomasrepcik.sensorbox.measurements.storage

internal fun sensorSeriesFormat(header: List<String>): SensorSeriesFormat {
    val unixTimestampIndex = header.indexOf("t_unix").takeIf { it >= 0 }
    val sensorTimestampIndex = header.indexOfFirst { it == "t_sensor" || it == "t_nanos" }
        .takeIf { it >= 0 }
    val elapsedTimestampIndex = header.indexOf("t_elapsed").takeIf { it >= 0 }
    val timestampIndex = unixTimestampIndex ?: sensorTimestampIndex ?: elapsedTimestampIndex ?: 0
    val valueIndexes = header.indices.filter { index ->
        index != timestampIndex && header[index] !in NON_VALUE_COLUMNS
    }
    return SensorSeriesFormat(
        columns = valueIndexes.map(header::get),
        timestampIndex = timestampIndex,
        valueIndexes = valueIndexes,
        usesSensorTimestamp = unixTimestampIndex == null && sensorTimestampIndex != null,
        usesElapsedTimestamp = unixTimestampIndex == null && elapsedTimestampIndex != null,
    )
}

internal fun parseSensorSample(
    line: String,
    format: SensorSeriesFormat,
    anchor: SensorTimeAnchor?,
): SensorSeriesSample? {
    val fields = line.split(DELIMITER)
    val rawTimestamp = fields.getOrNull(format.timestampIndex)?.toLongOrNull() ?: return null
    val timestamp = if (format.usesSensorTimestamp) {
        anchor?.unixMillis?.plus((rawTimestamp - anchor.elapsedRealtimeNanos) / NANOS_PER_MILLISECOND)
            ?: (rawTimestamp / NANOS_PER_MILLISECOND)
    } else if (format.usesElapsedTimestamp) {
        anchor?.unixMillis?.plus(rawTimestamp - anchor.elapsedRealtimeNanos / NANOS_PER_MILLISECOND)
            ?: rawTimestamp
    } else {
        rawTimestamp
    }
    val values = format.valueIndexes.mapNotNull { fields.getOrNull(it)?.toDoubleOrNull() }
    return values.takeIf { it.size == format.valueIndexes.size }?.let { SensorSeriesSample(timestamp, it) }
}

internal data class SensorTimeAnchor(val unixMillis: Long, val elapsedRealtimeNanos: Long)

internal data class SensorSeriesFormat(
    val columns: List<String>,
    val timestampIndex: Int,
    val valueIndexes: List<Int>,
    val usesSensorTimestamp: Boolean,
    val usesElapsedTimestamp: Boolean,
)

private const val DELIMITER = ';'
private const val NANOS_PER_MILLISECOND = 1_000_000L
private val NON_VALUE_COLUMNS = setOf("t_unix", "t_sensor", "t_nanos", "t_elapsed", "accuracy", "provider")
