package com.tomasrepcik.sensorbox.measurements.storage

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.suspendAppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject

class AndroidMeasurementRepository @Inject constructor(@ApplicationContext private val context: Context) :
    MeasurementRepository {
    override suspend fun loadMeasurements(): AppResult<List<MeasurementSummary>> = withContext(Dispatchers.IO) {
        suspendAppResult(AppErrorCode.STORAGE, "List saved measurements") {
            selectedMeasurementsDirectory().listFiles()
                .filter(DocumentFile::isDirectory)
                .map(::createMeasurementSummary)
                .sortedWith(
                    compareByDescending<MeasurementSummary> { it.recordedAtMillis ?: Long.MIN_VALUE }
                        .thenByDescending(MeasurementSummary::name),
                )
        }
    }

    override suspend fun loadMeasurementDetails(measurementId: String): AppResult<MeasurementDetails> =
        withContext(Dispatchers.IO) {
            suspendAppResult(AppErrorCode.STORAGE, "Read measurement details") {
                val directory = measurementDirectory(measurementId)
                val metadataFile = directory.findFile(METADATA_FILE)?.takeIf(DocumentFile::isFile)
                val metadata = metadataFile?.let(::readDocumentText).orEmpty()
                val parsedMetadata = MeasurementMetadataParser.parse(metadata)
                val files = directory.listFiles()
                    .filter { it.isFile && it.name != METADATA_FILE }
                    .mapNotNull(::createMeasurementFileSummary)
                    .sortedWith(compareBy(MeasurementFileSummary::kind, MeasurementFileSummary::name))
                MeasurementDetails(
                    summary = createMeasurementSummary(directory),
                    metadata = parsedMetadata.session,
                    files = files,
                    sensorMetadataByFile = parsedMetadata.bySensorFile,
                )
            }
        }

    override suspend fun loadMeasurementFile(measurementId: String, fileId: String): AppResult<MeasurementFileContent> =
        withContext(Dispatchers.IO) {
            suspendAppResult(AppErrorCode.STORAGE, "Read measurement file") {
                val directory = measurementDirectory(measurementId)
                val document = directory.findFile(fileId)
                    ?.takeIf(DocumentFile::isFile)
                    ?: error("Measurement file is unavailable")
                when {
                    document.name.equals(GPS_FILE, ignoreCase = true) -> parseGpsCoordinates(document)

                    document.name.orEmpty().endsWith(CSV_EXTENSION, ignoreCase = true) ->
                        parseSensorSeries(document, readSensorTimeAnchor(directory))

                    else -> parseTextFile(document)
                }
            }
        }

    private fun selectedMeasurementsDirectory(): DocumentFile {
        val permission = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .maxByOrNull { it.persistedTime }
            ?: throw IllegalStateException("Recording archive is not configured")
        return DocumentFile.fromTreeUri(context, permission.uri)
            ?.takeIf(DocumentFile::isDirectory)
            ?: throw IllegalStateException("Recording archive is unavailable")
    }

    private fun measurementDirectory(measurementId: String): DocumentFile = selectedMeasurementsDirectory().listFiles()
        .firstOrNull { it.isDirectory && it.name == measurementId }
        ?: throw IllegalArgumentException("Measurement does not exist")

    private fun createMeasurementSummary(directory: DocumentFile): MeasurementSummary {
        val json = readMetadata(directory)
        return MeasurementSummary(
            id = checkNotNull(directory.name),
            name = directory.name.orEmpty(),
            recordedAtMillis = json?.get("millis")?.asPrimitive()?.longOrNull,
            recordedAtText = json?.get("date")?.asPrimitive()?.contentOrNull,
            fileCount = directory.listFiles().count { it.isFile && it.name != METADATA_FILE },
        )
    }

    private fun createMeasurementFileSummary(document: DocumentFile): MeasurementFileSummary? {
        val name = document.name ?: return null
        val kind = when {
            name.equals(GPS_FILE, ignoreCase = true) -> MeasurementFileKind.GPS
            name.endsWith(CSV_EXTENSION, ignoreCase = true) -> MeasurementFileKind.SENSOR
            else -> MeasurementFileKind.TEXT
        }
        return MeasurementFileSummary(name, formatFileDisplayName(name), kind, document.length())
    }

    private fun parseSensorSeries(
        document: DocumentFile,
        anchor: SensorTimeAnchor?,
    ): MeasurementFileContent.SensorSeries {
        val samples = mutableListOf<SensorSeriesSample>()
        var totalSamples = 0

        val columns = readSensorSamples(document, anchor) { sample ->
            totalSamples += 1
            if (samples.size < MAX_CHART_SAMPLES) samples += sample
        }

        if (totalSamples <= MAX_CHART_SAMPLES) {
            return MeasurementFileContent.SensorSeries(columns, samples, truncated = false)
        }

        val selectedIndexes = chartSampleIndexes(totalSamples, MAX_CHART_SAMPLES)
        samples.clear()
        var sampleIndex = 0
        var selectedIndex = 0

        readSensorSamples(document, anchor) { sample ->
            if (selectedIndex < selectedIndexes.size && sampleIndex == selectedIndexes[selectedIndex]) {
                samples += sample
                selectedIndex += 1
            }
            sampleIndex += 1
        }

        return MeasurementFileContent.SensorSeries(columns, samples, truncated = true)
    }

    private fun readSensorSamples(
        document: DocumentFile,
        anchor: SensorTimeAnchor?,
        onSample: (SensorSeriesSample) -> Unit,
    ): List<String> {
        var columns = emptyList<String>()
        openDocumentStream(document).bufferedReader().useLines { lines ->
            val iterator = lines.iterator()
            if (!iterator.hasNext()) return@useLines

            val format = sensorSeriesFormat(iterator.next().split(DELIMITER))
            columns = format.columns

            while (iterator.hasNext()) {
                parseSensorSample(
                    line = iterator.next(),
                    format = format,
                    anchor = anchor,
                )?.let(onSample)
            }
        }
        return columns
    }

    private fun sensorSeriesFormat(header: List<String>): SensorSeriesFormat {
        val unixTimestampIndex = header.indexOf("t_unix").takeIf { it >= 0 }
        val sensorTimestampIndex = header.indexOf("t_sensor").takeIf { it >= 0 }
        val timestampIndex = unixTimestampIndex ?: sensorTimestampIndex ?: 0
        val valueIndexes = header.indices.filter { index ->
            index != timestampIndex && header[index] !in NON_VALUE_COLUMNS
        }
        return SensorSeriesFormat(
            columns = valueIndexes.map(header::get),
            timestampIndex = timestampIndex,
            valueIndexes = valueIndexes,
            usesSensorTimestamp = sensorTimestampIndex != null,
        )
    }

    private fun parseSensorSample(
        line: String,
        format: SensorSeriesFormat,
        anchor: SensorTimeAnchor?,
    ): SensorSeriesSample? {
        val fields = line.split(DELIMITER)
        val rawTimestamp = fields.getOrNull(format.timestampIndex)?.toLongOrNull() ?: return null
        val timestamp = if (format.usesSensorTimestamp) {
            anchor?.unixMillis?.plus((rawTimestamp - anchor.elapsedRealtimeNanos) / NANOS_PER_MILLISECOND)
                ?: (rawTimestamp / NANOS_PER_MILLISECOND)
        } else {
            rawTimestamp
        }
        val values = format.valueIndexes.mapNotNull { fields.getOrNull(it)?.toDoubleOrNull() }
        return values.takeIf { it.size == format.valueIndexes.size }?.let { SensorSeriesSample(timestamp, it) }
    }

    private fun readSensorTimeAnchor(directory: DocumentFile): SensorTimeAnchor? {
        val json = readMetadata(directory)
        val unixMillis = json?.get("millis")?.asPrimitive()?.longOrNull
        val elapsedRealtimeNanos = json?.get("nanos")?.asPrimitive()?.longOrNull
        return if (unixMillis != null && elapsedRealtimeNanos != null) {
            SensorTimeAnchor(unixMillis, elapsedRealtimeNanos)
        } else {
            null
        }
    }

    private fun readMetadata(directory: DocumentFile): JsonObject? = directory.findFile(METADATA_FILE)
        ?.takeIf(DocumentFile::isFile)
        ?.let(::readDocumentText)
        ?.takeIf(String::isNotBlank)
        ?.let(::parseMetadataObject)

    private fun parseGpsCoordinates(document: DocumentFile): MeasurementFileContent.GpsCoordinates {
        val coordinates = mutableListOf<GpsCoordinate>()
        var totalCoordinates = 0
        openDocumentStream(document).bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->
                val fields = line.split(DELIMITER)
                val coordinate = GpsCoordinate(
                    timestampMillis = fields.getOrNull(0)?.toLongOrNull() ?: return@forEach,
                    latitude = fields.getOrNull(1)?.toDoubleOrNull() ?: return@forEach,
                    longitude = fields.getOrNull(2)?.toDoubleOrNull() ?: return@forEach,
                    altitude = fields.getOrNull(3)?.toDoubleOrNull(),
                    accuracyMeters = fields.getOrNull(4)?.toDoubleOrNull(),
                    speedMetersPerSecond = fields.getOrNull(5)?.toDoubleOrNull(),
                    bearingDegrees = fields.getOrNull(6)?.toDoubleOrNull(),
                    provider = fields.getOrNull(7),
                )
                totalCoordinates += 1
                if (coordinates.size < MAX_GPS_COORDINATES) coordinates += coordinate
            }
        }
        return MeasurementFileContent.GpsCoordinates(
            coordinates,
            truncated = totalCoordinates > coordinates.size,
        )
    }

    private fun parseTextFile(document: DocumentFile): MeasurementFileContent.Text {
        val text = openDocumentStream(document).bufferedReader().use { reader ->
            buildString {
                val buffer = CharArray(TEXT_BUFFER_SIZE)
                var remaining = MAX_TEXT_CHARACTERS
                while (remaining > 0) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, remaining))
                    if (count < 0) break
                    appendRange(buffer, 0, count)
                    remaining -= count
                }
            }
        }
        return MeasurementFileContent.Text(text, truncated = document.length() > text.length)
    }

    private fun readDocumentText(document: DocumentFile): String = openDocumentStream(document)
        .bufferedReader()
        .use { it.readText() }

    private fun openDocumentStream(document: DocumentFile) = context.contentResolver.openInputStream(document.uri)
        ?: throw IllegalStateException("Measurement file cannot be opened")

    private fun parseMetadataObject(value: String): JsonObject = JSON.parseToJsonElement(value).jsonObject

    private fun JsonElement.asPrimitive(): JsonPrimitive? = this as? JsonPrimitive

    private fun formatFileDisplayName(value: String): String = value.substringBeforeLast('.')
        .replace('_', ' ')
        .replaceFirstChar(Char::titlecase)

    private companion object {
        const val METADATA_FILE = "extra.json"
        const val GPS_FILE = "gps.csv"
        const val CSV_EXTENSION = ".csv"
        const val DELIMITER = ';'
        const val MAX_CHART_SAMPLES = 2_000
        const val MAX_GPS_COORDINATES = 10_000
        const val MAX_TEXT_CHARACTERS = 100_000
        const val TEXT_BUFFER_SIZE = 4_096
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val NON_VALUE_COLUMNS = setOf("t_sensor", "accuracy", "provider")
        val JSON = Json { ignoreUnknownKeys = true }
    }

    private data class SensorTimeAnchor(val unixMillis: Long, val elapsedRealtimeNanos: Long)

    private data class SensorSeriesFormat(
        val columns: List<String>,
        val timestampIndex: Int,
        val valueIndexes: List<Int>,
        val usesSensorTimestamp: Boolean,
    )
}

internal fun chartSampleIndexes(totalSamples: Int, limit: Int): IntArray {
    require(totalSamples >= 0)
    require(limit > 0)

    val selectedSamples = minOf(totalSamples, limit)
    if (selectedSamples == 0) return IntArray(0)
    if (selectedSamples == 1) return intArrayOf(0)

    val lastSampleIndex = totalSamples - 1L
    val lastSelectedIndex = selectedSamples - 1L
    return IntArray(selectedSamples) { selectedIndex ->
        (selectedIndex * lastSampleIndex / lastSelectedIndex).toInt()
    }
}
