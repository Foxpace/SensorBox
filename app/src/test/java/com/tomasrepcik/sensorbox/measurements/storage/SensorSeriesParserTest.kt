package com.tomasrepcik.sensorbox.measurements.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorSeriesParserTest {
    private val anchor = SensorTimeAnchor(unixMillis = 1_700_000_000_000L, elapsedRealtimeNanos = 10_000_000_000L)

    @Test
    fun `Given activity updates When parsed Then elapsed milliseconds become recording wall time`() {
        // Given
        val format = sensorSeriesFormat(listOf("t_elapsed", "still", "walking"))

        // When
        val sample = parseSensorSample("12000;20;80", format, anchor)

        // Then
        assertEquals(SensorSeriesSample(1_700_000_002_000L, listOf(20.0, 80.0)), sample)
    }

    @Test
    fun `Given activity transitions When parsed Then nanoseconds become recording wall time`() {
        // Given
        val format = sensorSeriesFormat(listOf("t_nanos", "activity", "enter_exit"))

        // When
        val sample = parseSensorSample("12000000000;7;0", format, anchor)

        // Then
        assertEquals(SensorSeriesSample(1_700_000_002_000L, listOf(7.0, 0.0)), sample)
    }

    @Test
    fun `Given both timestamp columns When parsed Then Unix time takes precedence`() {
        // Given
        val format = sensorSeriesFormat(listOf("t_unix", "t_sensor", "step", "accuracy"))

        // When
        val sample = parseSensorSample("1700000002000;12000000000;1;3", format, anchor)

        // Then
        assertEquals(listOf("step"), format.columns)
        assertEquals(SensorSeriesSample(1_700_000_002_000L, listOf(1.0)), sample)
    }

    @Test
    fun `Given motion trigger When parsed Then its timestamp and event are preserved`() {
        // Given
        val format = sensorSeriesFormat(listOf("t_sensor", "event"))

        // When
        val sample = parseSensorSample("12000000000;1", format, anchor)

        // Then
        assertEquals(SensorSeriesSample(1_700_000_002_000L, listOf(1.0)), sample)
    }
}
