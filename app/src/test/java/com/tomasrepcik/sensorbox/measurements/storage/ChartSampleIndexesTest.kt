package com.tomasrepcik.sensorbox.measurements.storage

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ChartSampleIndexesTest {
    @Test
    fun `Given excess samples When indexes are selected Then the complete recording is represented`() {
        // Given
        val totalSamples = 10
        val chartLimit = 4

        // When
        val indexes = chartSampleIndexes(totalSamples, chartLimit)

        // Then
        assertArrayEquals(intArrayOf(0, 3, 6, 9), indexes)
    }

    @Test
    fun `Given fewer samples than the chart limit When indexes are selected Then every sample is represented`() {
        // Given
        val totalSamples = 4
        val chartLimit = 10

        // When
        val indexes = chartSampleIndexes(totalSamples, chartLimit)

        // Then
        assertArrayEquals(intArrayOf(0, 1, 2, 3), indexes)
    }
}
