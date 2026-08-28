package com.tomasrepcik.sensorbox.domain.sensors

import android.hardware.Sensor
import org.junit.Assert.assertFalse
import org.junit.Test

class WatchSensorTypesTest {
    @Test
    fun `Given watch sensor discovery When supported types are inspected Then heart rate is absent`() {
        assertFalse(Sensor.TYPE_HEART_RATE in WEAR_SENSOR_TYPES)
    }
}
