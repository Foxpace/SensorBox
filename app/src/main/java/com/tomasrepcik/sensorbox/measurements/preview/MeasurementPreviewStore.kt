package com.tomasrepcik.sensorbox.measurements.preview

import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileContent
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileSummary
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementMetadataEntry
import javax.inject.Inject
import javax.inject.Singleton

data class MeasurementPreviewData(
    val file: MeasurementFileSummary,
    val content: MeasurementFileContent,
    val sensorMetadata: List<MeasurementMetadataEntry>,
)

@Singleton
class MeasurementPreviewStore @Inject constructor() {
    private val previews = mutableMapOf<MeasurementPreviewKey, MeasurementPreviewData>()

    @Synchronized
    fun put(measurementId: String, fileId: String, data: MeasurementPreviewData) {
        previews[MeasurementPreviewKey(measurementId, fileId)] = data
    }

    @Synchronized
    fun take(measurementId: String, fileId: String): MeasurementPreviewData? =
        previews.remove(MeasurementPreviewKey(measurementId, fileId))
}

private data class MeasurementPreviewKey(val measurementId: String, val fileId: String)
