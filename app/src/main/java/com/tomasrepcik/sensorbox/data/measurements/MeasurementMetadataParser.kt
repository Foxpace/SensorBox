package com.tomasrepcik.sensorbox.data.measurements

import com.tomasrepcik.sensorbox.domain.measurements.MeasurementMetadataEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

internal object MeasurementMetadataParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(value: String): ParsedMeasurementMetadata {
        if (value.isBlank()) return ParsedMeasurementMetadata.EMPTY
        val root = json.parseToJsonElement(value).jsonObject
        return ParsedMeasurementMetadata(
            session = flatten(JsonObject(root.filterKeys { key -> key !in SENSOR_KEYS })),
            bySensorFile = sensorMetadata(root),
        )
    }

    private fun sensorMetadata(root: JsonObject): Map<String, List<MeasurementMetadataEntry>> {
        val ranges = root[RANGES_KEY] as? JsonArray ?: JsonArray(emptyList())
        val sensorFiles = root[SENSOR_FILES_KEY] as? JsonArray ?: JsonArray(emptyList())
        return sensorFiles.mapIndexedNotNull { index, element ->
            val file = element as? JsonObject ?: return@mapIndexedNotNull null
            val fileName = (file[FILE_NAME_KEY] as? JsonPrimitive)?.contentOrNull
                ?: return@mapIndexedNotNull null
            val details = buildList {
                ranges.getOrNull(index)?.let { addAll(flatten(it)) }
                addAll(flatten(JsonObject(file.filterKeys { key -> key != FILE_NAME_KEY })))
            }
            fileName to details
        }.toMap()
    }

    private fun flatten(element: JsonElement): List<MeasurementMetadataEntry> = buildList {
        collect(element, prefix = "", destination = this)
    }

    private fun collect(element: JsonElement, prefix: String, destination: MutableList<MeasurementMetadataEntry>) {
        when (element) {
            is JsonObject -> element.forEach { (key, value) ->
                collect(value, prefix.appendKey(key), destination)
            }

            is JsonArray -> element.forEachIndexed { index, value ->
                collect(value, "$prefix[$index]", destination)
            }

            is JsonPrimitive -> destination += MeasurementMetadataEntry(prefix, element.content)
        }
    }

    private fun String.appendKey(value: String): String = if (isEmpty()) value else "$this.$value"

    private const val RANGES_KEY = "ranges"
    private const val SENSOR_FILES_KEY = "sensorFiles"
    private const val FILE_NAME_KEY = "fileName"
    private val SENSOR_KEYS = setOf(RANGES_KEY, SENSOR_FILES_KEY)
}

internal data class ParsedMeasurementMetadata(
    val session: List<MeasurementMetadataEntry>,
    val bySensorFile: Map<String, List<MeasurementMetadataEntry>>,
) {
    companion object {
        val EMPTY = ParsedMeasurementMetadata(emptyList(), emptyMap())
    }
}
