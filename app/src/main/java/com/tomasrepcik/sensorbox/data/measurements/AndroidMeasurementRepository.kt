package com.tomasrepcik.sensorbox.data.measurements

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.suspendAppResult
import com.tomasrepcik.sensorbox.domain.measurements.GpsCoordinate
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementDetails
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileContent
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileKind
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileSummary
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementRepository
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementSummary
import com.tomasrepcik.sensorbox.domain.measurements.SensorSeriesSample
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
        var columns = emptyList<String>()
        val samples = mutableListOf<SensorSeriesSample>()
        var totalSamples = 0
        openDocumentStream(document).bufferedReader().useLines { lines ->
            val iterator = lines.iterator()
            if (!iterator.hasNext()) return@useLines
            val header = iterator.next().split(DELIMITER)
            val unixTimestampIndex = header.indexOf("t_unix").takeIf { it >= 0 }
            val sensorTimestampIndex = header.indexOf("t_sensor").takeIf { it >= 0 }
            val timestampIndex = unixTimestampIndex ?: sensorTimestampIndex ?: 0
            val valueIndexes = header.indices.filter { index ->
                index != timestampIndex && header[index] !in NON_VALUE_COLUMNS
            }
            columns = valueIndexes.map(header::get)
            while (iterator.hasNext()) {
                parseSensorSample(
                    line = iterator.next(),
                    timestampIndex = timestampIndex,
                    valueIndexes = valueIndexes,
                    sensorTimestamp = sensorTimestampIndex != null,
                    anchor = anchor,
                )?.let { sample ->
                    totalSamples += 1
                    retainBounded(samples, sample, totalSamples, MAX_CHART_SAMPLES)
                }
            }
        }
        return MeasurementFileContent.SensorSeries(columns, samples, totalSamples > samples.size)
    }

    private fun parseSensorSample(
        line: String,
        timestampIndex: Int,
        valueIndexes: List<Int>,
        sensorTimestamp: Boolean,
        anchor: SensorTimeAnchor?,
    ): SensorSeriesSample? {
        val fields = line.split(DELIMITER)
        val rawTimestamp = fields.getOrNull(timestampIndex)?.toLongOrNull() ?: return null
        val timestamp = if (sensorTimestamp) {
            anchor?.unixMillis?.plus((rawTimestamp - anchor.elapsedRealtimeNanos) / NANOS_PER_MILLISECOND)
                ?: rawTimestamp / NANOS_PER_MILLISECOND
        } else {
            rawTimestamp
        }
        val values = valueIndexes.mapNotNull { fields.getOrNull(it)?.toDoubleOrNull() }
        return values.takeIf { it.size == valueIndexes.size }?.let { SensorSeriesSample(timestamp, it) }
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
                    append(buffer, 0, count)
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

    private fun <T> retainBounded(destination: MutableList<T>, value: T, count: Int, limit: Int) {
        if (destination.size < limit) {
            destination += value
        } else if (count % SAMPLE_REPLACEMENT_INTERVAL == 0) {
            destination.removeAt(destination.lastIndex)
            destination += value
        }
    }

    private companion object {
        const val METADATA_FILE = "extra.json"
        const val GPS_FILE = "gps.csv"
        const val CSV_EXTENSION = ".csv"
        const val DELIMITER = ';'
        const val MAX_CHART_SAMPLES = 2_000
        const val MAX_GPS_COORDINATES = 10_000
        const val MAX_TEXT_CHARACTERS = 100_000
        const val TEXT_BUFFER_SIZE = 4_096
        const val SAMPLE_REPLACEMENT_INTERVAL = 100
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val NON_VALUE_COLUMNS = setOf("t_sensor", "accuracy", "provider")
        val JSON = Json { ignoreUnknownKeys = true }
    }

    private data class SensorTimeAnchor(val unixMillis: Long, val elapsedRealtimeNanos: Long)
}
