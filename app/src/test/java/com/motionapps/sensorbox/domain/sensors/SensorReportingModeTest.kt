package com.motionapps.sensorbox.domain.sensors

import android.hardware.Sensor
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorReportingModeTest {
    @Test
    fun `Given Android reporting modes When mapped Then every value and unknown are represented`() {
        assertEquals(SensorReportingMode.CONTINUOUS, Sensor.REPORTING_MODE_CONTINUOUS.toSensorReportingMode())
        assertEquals(SensorReportingMode.ON_CHANGE, Sensor.REPORTING_MODE_ON_CHANGE.toSensorReportingMode())
        assertEquals(SensorReportingMode.ONE_SHOT, Sensor.REPORTING_MODE_ONE_SHOT.toSensorReportingMode())
        assertEquals(SensorReportingMode.SPECIAL_TRIGGER, Sensor.REPORTING_MODE_SPECIAL_TRIGGER.toSensorReportingMode())
        assertEquals(SensorReportingMode.UNKNOWN, Int.MAX_VALUE.toSensorReportingMode())
    }
}
