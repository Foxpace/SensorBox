package com.tomasrepcik.sensorbox.data.measurements

import com.tomasrepcik.sensorbox.domain.measurements.MeasurementMetadataEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MeasurementMetadataParserTest {
    @Test
    fun `Given several sensors When metadata is parsed Then sensor details leave the session summary`() {
        // Given
        val metadata = """
            {
              "folder": "Morning walk",
              "stopReason": "DURATION_EXPIRED",
              "ranges": [
                {"sensor": "Bosch accelerometer", "type": 1, "range": 78.4},
                {"sensor": "Bosch gyroscope", "type": 4, "range": 34.9}
              ],
              "sensorFiles": [
                {"fileName": "accelerometer.csv", "writtenSamples": 120, "droppedSamples": 0},
                {"fileName": "gyroscope.csv", "writtenSamples": 118, "droppedSamples": 2}
              ]
            }
        """.trimIndent()

        // When
        val parsed = MeasurementMetadataParser.parse(metadata)

        // Then
        assertEquals(
            listOf(
                MeasurementMetadataEntry("folder", "Morning walk"),
                MeasurementMetadataEntry("stopReason", "DURATION_EXPIRED"),
            ),
            parsed.session,
        )
        assertFalse(parsed.session.any { entry -> entry.name.startsWith("ranges") })
        assertEquals(
            listOf(
                MeasurementMetadataEntry("sensor", "Bosch accelerometer"),
                MeasurementMetadataEntry("type", "1"),
                MeasurementMetadataEntry("range", "78.4"),
                MeasurementMetadataEntry("writtenSamples", "120"),
                MeasurementMetadataEntry("droppedSamples", "0"),
            ),
            parsed.bySensorFile.getValue("accelerometer.csv"),
        )
        assertEquals("Bosch gyroscope", parsed.bySensorFile.getValue("gyroscope.csv").first().value)
    }

    @Test
    fun `Given no metadata When parsed Then both detail groups are empty`() {
        // Given
        val metadata = ""

        // When
        val parsed = MeasurementMetadataParser.parse(metadata)

        // Then
        assertEquals(ParsedMeasurementMetadata.EMPTY, parsed)
    }
}
