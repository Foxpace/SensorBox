package com.motionapps.sensorbox.domain.measurement

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class WearMeasurementPermissionUseCaseTest {
    @Test
    fun `Given Wear GPS When permissions are derived Then only notification and location are requested`() {
        assertEquals(
            setOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_FINE_LOCATION),
            requiredWearMeasurementPermissions(includesGps = true, sdkInt = 37),
        )
    }
}
