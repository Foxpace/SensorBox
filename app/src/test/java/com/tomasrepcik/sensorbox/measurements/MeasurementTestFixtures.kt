package com.tomasrepcik.sensorbox.measurements

import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementDetails
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileContent
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileKind
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileSummary
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementMetadataEntry
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementRepository
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementSummary
import com.tomasrepcik.sensorbox.measurements.storage.SensorSeriesSample
import kotlinx.coroutines.CompletableDeferred

internal object MeasurementTestFixtures {
    val summary = MeasurementSummary("session-1", "Morning walk", 1_725_000_000_000, "2026-08-25", 1)
    val sensorFile = MeasurementFileSummary(
        "accelerometer.csv",
        "Accelerometer",
        MeasurementFileKind.SENSOR,
        8_192,
    )
    val sensorMetadata = listOf(MeasurementMetadataEntry("sensor", "Bosch accelerometer"))
    val details = MeasurementDetails(
        summary = summary,
        metadata = listOf(MeasurementMetadataEntry("device.model", "Pixel fixture")),
        files = listOf(sensorFile),
        sensorMetadataByFile = mapOf(sensorFile.id to sensorMetadata),
    )
    val sensorContent = MeasurementFileContent.SensorSeries(
        columns = listOf("x", "y", "z"),
        samples = listOf(
            SensorSeriesSample(1, listOf(0.1, 0.2, 0.3)),
            SensorSeriesSample(2, listOf(0.4, 0.5, 0.6)),
        ),
        truncated = false,
    )
}

internal class FakeMeasurementRepository : MeasurementRepository {
    var measurementsResult: AppResult<List<MeasurementSummary>> =
        AppResult.success(listOf(MeasurementTestFixtures.summary))
    var detailsResult: AppResult<MeasurementDetails> = AppResult.success(MeasurementTestFixtures.details)
    var detailsLoadCount = 0
    var fileResult: AppResult<MeasurementFileContent> = AppResult.success(MeasurementTestFixtures.sensorContent)
    var fileLoadGate: CompletableDeferred<Unit>? = null
    var fileProgress = listOf(1f)

    override suspend fun loadMeasurements(): AppResult<List<MeasurementSummary>> = measurementsResult

    override suspend fun loadMeasurementDetails(measurementId: String): AppResult<MeasurementDetails> {
        detailsLoadCount += 1
        return detailsResult
    }

    override suspend fun loadMeasurementFile(
        measurementId: String,
        fileId: String,
        onProgress: (Float) -> Unit,
    ): AppResult<MeasurementFileContent> {
        fileProgress.forEach(onProgress)
        fileLoadGate?.await()
        return fileResult
    }
}

internal fun measurementFailureStore() = AppFailureStore { }
