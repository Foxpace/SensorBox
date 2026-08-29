package com.tomasrepcik.sensorbox.domain.measurements

import com.tomasrepcik.sensorbox.core.error.AppResult

interface MeasurementRepository {
    suspend fun loadMeasurements(): AppResult<List<MeasurementSummary>>

    suspend fun loadMeasurementDetails(measurementId: String): AppResult<MeasurementDetails>

    suspend fun loadMeasurementFile(measurementId: String, fileId: String): AppResult<MeasurementFileContent>
}

data class MeasurementSummary(
    val id: String,
    val name: String,
    val recordedAtMillis: Long?,
    val recordedAtText: String?,
    val fileCount: Int,
)

data class MeasurementDetails(
    val summary: MeasurementSummary,
    val metadata: List<MeasurementMetadataEntry>,
    val files: List<MeasurementFileSummary>,
    val sensorMetadataByFile: Map<String, List<MeasurementMetadataEntry>> = emptyMap(),
)

data class MeasurementMetadataEntry(val name: String, val value: String)

data class MeasurementFileSummary(val id: String, val name: String, val kind: MeasurementFileKind, val sizeBytes: Long)

enum class MeasurementFileKind { SENSOR, GPS, TEXT }

sealed interface MeasurementFileContent {
    data class SensorSeries(val columns: List<String>, val samples: List<SensorSeriesSample>, val truncated: Boolean) :
        MeasurementFileContent

    data class GpsCoordinates(val coordinates: List<GpsCoordinate>, val truncated: Boolean) : MeasurementFileContent

    data class Text(val value: String, val truncated: Boolean) : MeasurementFileContent
}

data class SensorSeriesSample(val timestampMillis: Long, val values: List<Double>)

data class GpsCoordinate(
    val timestampMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracyMeters: Double?,
    val speedMetersPerSecond: Double?,
    val bearingDegrees: Double?,
    val provider: String?,
)
