package com.tomasrepcik.sensorbox.domain.measurement

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class WearMeasurementPermissionUseCaseTest {
    @Test
    fun `Given Wear GPS When permissions are derived Then only location is required`() {
        assertEquals(
            setOf(Manifest.permission.ACCESS_FINE_LOCATION),
            requiredWearMeasurementPermissions(includesGps = true),
        )
    }

    @Test
    fun `Given Wear sensors without GPS When permissions are derived Then none are required`() {
        assertEquals(
            emptySet<String>(),
            requiredWearMeasurementPermissions(includesGps = false),
        )
    }
}
