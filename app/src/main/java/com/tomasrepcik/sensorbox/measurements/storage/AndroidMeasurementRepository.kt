package com.tomasrepcik.sensorbox.measurements.storage

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.suspendAppResult
import com.tomasrepcik.sensorbox.core.storage.BACKUP_MEASUREMENT_PREFIX
import com.tomasrepcik.sensorbox.core.storage.PENDING_MEASUREMENT_PREFIX
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
import java.io.FilterInputStream
import java.io.InputStream
import javax.inject.Inject

class AndroidMeasurementRepository @Inject constructor(@ApplicationContext private val context: Context) :
    MeasurementRepository {
    override suspend fun loadMeasurements(): AppResult<List<MeasurementSummary>> = withContext(Dispatchers.IO) {
        suspendAppResult(AppErrorCode.STORAGE, "List saved measurements") {
            selectedMeasurementsDirectory().listFiles()
                .filter {
                    it.isDirectory &&
                        !it.name.orEmpty().startsWith(
                            PENDING_MEASUREMENT_PREFIX,
                        ) &&
                        !it.name.orEmpty().startsWith(BACKUP_MEASUREMENT_PREFIX)
                }
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

    override suspend fun loadMeasurementFile(
        measurementId: String,
        fileId: String,
        onProgress: (Float) -> Unit,
    ): AppResult<MeasurementFileContent> = withContext(Dispatchers.IO) {
        suspendAppResult(AppErrorCode.STORAGE, "Read measurement file") {
            val directory = measurementDirectory(measurementId)
            val document = directory.findFile(fileId)
                ?.takeIf(DocumentFile::isFile)
                ?: error("Measurement file is unavailable")
            when {
                document.name.equals(GPS_FILE, ignoreCase = true) -> parseGpsCoordinates(document, onProgress)

                document.name.orEmpty().endsWith(CSV_EXTENSION, ignoreCase = true) ->
                    parseSensorSeries(document, readSensorTimeAnchor(directory), onProgress)

                else -> parseTextFile(document, onProgress)
            }
        }
    }

    private fun selectedMeasurementsDirectory(): DocumentFile {
        val permission = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission }
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
            sessionId = json?.get("sessionId")?.asPrimitive()?.contentOrNull?.takeIf(String::isNotBlank),
            device = json?.get("device")?.asPrimitive()?.contentOrNull,
            recordingName = json?.get("folder")?.asPrimitive()?.contentOrNull ?: directory.name.orEmpty(),
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
        val displayName = if (kind == MeasurementFileKind.GPS) "GPS" else formatFileDisplayName(name)
        return MeasurementFileSummary(name, displayName, kind, document.length())
    }

    private fun parseSensorSeries(
        document: DocumentFile,
        anchor: SensorTimeAnchor?,
        onProgress: (Float) -> Unit,
    ): MeasurementFileContent.SensorSeries {
        val samples = mutableListOf<SensorSeriesSample>()
        var totalSamples = 0

        val columns = readSensorSamples(
            document = document,
            anchor = anchor,
            onProgress = { progress -> onProgress(progress / 2f) },
            onSample = { sample ->
                totalSamples += 1
                if (samples.size < MAX_CHART_SAMPLES) samples += sample
            },
        )

        if (totalSamples <= MAX_CHART_SAMPLES) {
            onProgress(1f)
            return MeasurementFileContent.SensorSeries(columns, samples, truncated = false)
        }

        val selectedIndexes = chartSampleIndexes(totalSamples, MAX_CHART_SAMPLES)
        samples.clear()
        var sampleIndex = 0
        var selectedIndex = 0

        readSensorSamples(
            document = document,
            anchor = anchor,
            onProgress = { progress -> onProgress(0.5f + progress / 2f) },
            onSample = { sample ->
                if (selectedIndex < selectedIndexes.size && sampleIndex == selectedIndexes[selectedIndex]) {
                    samples += sample
                    selectedIndex += 1
                }
                sampleIndex += 1
            },
        )
        onProgress(1f)

        return MeasurementFileContent.SensorSeries(columns, samples, truncated = true)
    }

    private fun readSensorSamples(
        document: DocumentFile,
        anchor: SensorTimeAnchor?,
        onProgress: (Float) -> Unit,
        onSample: (SensorSeriesSample) -> Unit,
    ): List<String> {
        var columns = emptyList<String>()
        openDocumentStream(document, onProgress).bufferedReader().useLines { lines ->
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

    private fun parseGpsCoordinates(
        document: DocumentFile,
        onProgress: (Float) -> Unit,
    ): MeasurementFileContent.GpsCoordinates {
        val coordinates = mutableListOf<GpsCoordinate>()
        var totalCoordinates = 0
        openDocumentStream(document, onProgress).bufferedReader().useLines { lines ->
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
        onProgress(1f)
        return MeasurementFileContent.GpsCoordinates(
            coordinates,
            truncated = totalCoordinates > coordinates.size,
        )
    }

    private fun parseTextFile(document: DocumentFile, onProgress: (Float) -> Unit): MeasurementFileContent.Text {
        val text = openDocumentStream(document, onProgress).bufferedReader().use { reader ->
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
        onProgress(1f)
        return MeasurementFileContent.Text(text, truncated = document.length() > text.length)
    }

    private fun readDocumentText(document: DocumentFile): String = openDocumentStream(document)
        .bufferedReader()
        .use { it.readText() }

    private fun openDocumentStream(document: DocumentFile, onProgress: ((Float) -> Unit)? = null): InputStream {
        val stream = context.contentResolver.openInputStream(document.uri)
            ?: throw IllegalStateException("Measurement file cannot be opened")
        val sizeBytes = document.length()
        return if (onProgress != null && sizeBytes > 0L) {
            ProgressInputStream(stream, sizeBytes, onProgress)
        } else {
            stream
        }
    }

    private fun parseMetadataObject(value: String): JsonObject = JSON.parseToJsonElement(value).jsonObject

    private fun JsonElement.asPrimitive(): JsonPrimitive? = this as? JsonPrimitive

    private fun formatFileDisplayName(value: String): String = value.substringBeforeLast('.')
        .replace('_', ' ')
        .replaceFirstChar(Char::titlecase)

    private companion object {
        const val METADATA_FILE = "extra.json"
        const val GPS_FILE = "GPS.csv"
        const val CSV_EXTENSION = ".csv"
        const val DELIMITER = ';'
        const val MAX_CHART_SAMPLES = 2_000
        const val MAX_GPS_COORDINATES = 10_000
        const val MAX_TEXT_CHARACTERS = 100_000
        const val TEXT_BUFFER_SIZE = 4_096
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

private class ProgressInputStream(
    input: InputStream,
    private val sizeBytes: Long,
    private val onProgress: (Float) -> Unit,
) : FilterInputStream(input) {
    private var readBytes = 0L
    private var reportedPercent = -1

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) reportBytesRead(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count > 0) reportBytesRead(count)
        return count
    }

    private fun reportBytesRead(count: Int) {
        readBytes += count
        val progress = (readBytes.toFloat() / sizeBytes).coerceIn(0f, 1f)
        val percent = (progress * 100).toInt()
        if (percent > reportedPercent) {
            reportedPercent = percent
            onProgress(progress)
        }
    }
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
