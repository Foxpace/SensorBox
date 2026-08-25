package com.tomasrepcik.sensorbox.sensorservices.types

import org.junit.Assert.assertEquals
import org.junit.Test

class SensorSpecTest {
    @Test
    fun `Given supported sensors When inspecting formats Then identifiers and files are unique`() {
        val givenSensors = SensorSpec.entries

        val whenTypes = givenSensors.map(SensorSpec::type)
        val whenFiles = givenSensors.map(SensorSpec::fileName)

        assertEquals(givenSensors.size, whenTypes.toSet().size)
        assertEquals(givenSensors.size, whenFiles.toSet().size)
    }

    @Test
    fun `Given a sensor format When counting CSV columns Then every axis and timestamp is represented`() {
        SensorSpec.entries.forEach { givenSensor ->
            val whenColumnCount = givenSensor.header.trim().split(';').size

            assertEquals(givenSensor.axisCount + METADATA_COLUMNS, whenColumnCount)
        }
    }

    private companion object {
        const val METADATA_COLUMNS = 3
    }
}
