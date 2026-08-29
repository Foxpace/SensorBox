package com.tomasrepcik.sensorbox.recordinghost.storage

import com.tomasrepcik.sensorbox.recordinghost.sources.sensor.SensorFileStats
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
        val json = Json.parseToJsonElement(Json.encodeToString(metadataFixture())).jsonObject

        assertEquals(
            setOf(
                "millis",
                "nanos",
                "endedAtMillis",
                "endedAtNanos",
                "type",
                "date",
                "folder",
                "notes",
                "annotations",
                "ranges",
                "alarms",
                "configuredAlarmOffsetsSeconds",
                "durationMillis",
                "actualDurationMillis",
                "stopReason",
                "failureOperation",
                "failureMessage",
                "failures",
                "wakeLockEnabled",
                "sensorFiles",
                "activityRecognition",
                "significantMotion",
            ),
            json.keys,
        )
        assertEquals(100L, json.getValue("millis").jsonPrimitive.long)
        assertEquals(200L, json.getValue("nanos").jsonPrimitive.long)
        assertEquals(60_100L, json.getValue("endedAtMillis").jsonPrimitive.long)
        assertEquals("RECORDING", json.getValue("type").jsonPrimitive.content)
        assertEquals(60_000L, json.getValue("durationMillis").jsonPrimitive.long)
        assertEquals(60_000L, json.getValue("actualDurationMillis").jsonPrimitive.long)
        assertEquals("USER_REQUEST", json.getValue("stopReason").jsonPrimitive.content)
        val failure = json.getValue("failures").jsonArray.single().jsonObject
        assertEquals("STORAGE", failure.getValue("code").jsonPrimitive.content)
        assertEquals("Write accelerometer.csv", failure.getValue("operation").jsonPrimitive.content)
        assertEquals("note", json.getValue("notes").jsonArray.single().jsonPrimitive.content)
        val annotation = json.getValue("annotations").jsonArray.single().jsonObject
        val range = json.getValue("ranges").jsonArray.single().jsonObject
        assertEquals(300L, annotation.getValue("timestamp").jsonPrimitive.long)
        assertEquals("mark", annotation.getValue("annotation").jsonPrimitive.content)
        assertEquals(1, range.getValue("type").jsonPrimitive.int)
        assertEquals(9.81f, range.getValue("range").jsonPrimitive.float)
        assertEquals(true, json.getValue("activityRecognition").jsonPrimitive.boolean)
    }

    private fun metadataFixture() = MeasurementMetadata(
        millis = 100L,
        nanos = 200L,
        endedAtMillis = 60_100L,
        endedAtNanos = 60_000_000_200L,
        type = "RECORDING",
        date = "23. 08. 2026 12:00:00",
        folder = "fixture",
        notes = listOf("note"),
        annotations = listOf(MeasurementAnnotation(timestamp = 300L, annotation = "mark")),
        ranges = listOf(SensorRange(sensor = "Accelerometer", type = 1, range = 9.81f)),
        alarms = listOf(400L),
        configuredAlarmOffsetsSeconds = listOf(5),
        durationMillis = 60_000L,
        actualDurationMillis = 60_000L,
        stopReason = "USER_REQUEST",
        failureOperation = "Write accelerometer.csv",
        failureMessage = "Write accelerometer.csv failed with IOException",
        failures = listOf(
            MeasurementFailure(
                code = "STORAGE",
                operation = "Write accelerometer.csv",
                message = "Write accelerometer.csv failed with IOException",
                cause = "IOException: disk full",
                context = emptyMap(),
            ),
        ),
        wakeLockEnabled = true,
        sensorFiles = listOf(
            SensorFileStats(
                fileName = "accelerometer.csv",
                acceptedSamples = 10L,
                writtenSamples = 10L,
                droppedSamples = 0L,
                failureOperation = null,
            ),
        ),
        activityRecognition = true,
        significantMotion = false,
    )
}
