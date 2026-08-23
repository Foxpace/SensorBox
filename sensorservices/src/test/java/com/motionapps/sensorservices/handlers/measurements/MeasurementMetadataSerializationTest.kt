package com.motionapps.sensorservices.handlers.measurements

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementMetadataSerializationTest {
    @Test
    fun `Given measurement metadata When serialized Then historical keys and value types are preserved`() {
        val metadata = MeasurementMetadata(
            millis = 100L,
            nanos = 200L,
            type = "TIMED",
            date = "23. 08. 2026 12:00:00",
            folder = "fixture",
            notes = listOf("note"),
            annotations = listOf(MeasurementAnnotation(timestamp = 300L, annotation = "mark")),
            ranges = listOf(SensorRange(sensor = "Accelerometer", type = 1, range = 9.81f)),
            alarms = listOf(400L),
            configuredAlarmOffsetsSeconds = listOf(5),
            durationMillis = 60_000L,
            activityRecognition = true,
            significantMotion = false,
        )

        val json = Json.parseToJsonElement(Json.encodeToString(metadata)).jsonObject

        assertEquals(
            setOf(
                "millis",
                "nanos",
                "type",
                "date",
                "folder",
                "notes",
                "annotations",
                "ranges",
                "alarms",
                "configuredAlarmOffsetsSeconds",
                "durationMillis",
                "activityRecognition",
                "significantMotion",
            ),
            json.keys,
        )
        assertEquals(100L, json.getValue("millis").jsonPrimitive.long)
        assertEquals("note", json.getValue("notes").jsonArray.single().jsonPrimitive.content)
        val annotation = json.getValue("annotations").jsonArray.single().jsonObject
        val range = json.getValue("ranges").jsonArray.single().jsonObject
        assertEquals(300L, annotation.getValue("timestamp").jsonPrimitive.long)
        assertEquals("mark", annotation.getValue("annotation").jsonPrimitive.content)
        assertEquals(1, range.getValue("type").jsonPrimitive.int)
        assertEquals(9.81f, range.getValue("range").jsonPrimitive.float)
        assertEquals(true, json.getValue("activityRecognition").jsonPrimitive.boolean)
    }
}
