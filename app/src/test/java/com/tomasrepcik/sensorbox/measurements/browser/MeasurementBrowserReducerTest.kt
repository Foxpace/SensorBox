package com.tomasrepcik.sensorbox.measurements.browser

import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementBrowserReducerTest {
    @Test
    fun `zooming at chart center halves the visible time`() {
        val result = MeasurementBrowserReducer.zoomSensorChartTimeWindow(
            state = MeasurementBrowserState(),
            zoomFactor = 2f,
            focalPointFraction = 0.5f,
        )

        assertEquals(SensorChartTimeWindow(0.25f, 0.75f), result.sensorChartTimeWindow)
    }

    @Test
    fun `shifting chart time window cannot move beyond recording end`() {
        val state = MeasurementBrowserState(sensorChartTimeWindow = SensorChartTimeWindow(0.25f, 0.75f))

        val result = MeasurementBrowserReducer.shiftSensorChartTimeWindow(
            state = state,
            visibleWindowFraction = 10f,
        )

        assertEquals(SensorChartTimeWindow(0.5f, 1f), result.sensorChartTimeWindow)
    }

    @Test
    fun `showing entire chart restores the complete recording range`() {
        val state = MeasurementBrowserState(sensorChartTimeWindow = SensorChartTimeWindow(0.4f, 0.6f))

        val result = MeasurementBrowserReducer.showEntireSensorChartTimeRange(state)

        assertEquals(SensorChartTimeWindow(), result.sensorChartTimeWindow)
    }

    @Test
    fun `invalid chart zoom leaves current time window unchanged`() {
        val state = MeasurementBrowserState(sensorChartTimeWindow = SensorChartTimeWindow(0.2f, 0.8f))

        val result = MeasurementBrowserReducer.zoomSensorChartTimeWindow(
            state = state,
            zoomFactor = Float.NaN,
            focalPointFraction = 0.5f,
        )

        assertEquals(state, result)
    }
}
