package com.tomasrepcik.sensorbox.recording.sources

import android.hardware.Sensor
import org.junit.Assert.assertFalse
import org.junit.Test

class PhoneSensorTypesTest {
    @Test
    fun `Given phone sensor discovery When supported types are inspected Then heart rate is absent`() {
        assertFalse(Sensor.TYPE_HEART_RATE in PHONE_SENSOR_TYPES)
    }
}
