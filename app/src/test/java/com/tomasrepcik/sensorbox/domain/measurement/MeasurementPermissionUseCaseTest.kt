package com.tomasrepcik.sensorbox.domain.measurement

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementPermissionUseCaseTest {
    @Test
    fun `Given optional phone sources When permissions are derived Then only supported permissions are used`() {
        val request = request(includesGps = true, activityRecognition = true)

        assertEquals(
            setOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION,
            ),
            requiredMeasurementPermissions(request, sdkInt = 37),
        )
    }

    @Test
    fun `Given a legacy phone When permissions are derived Then version gated permissions are absent`() {
        assertEquals(
            setOf(Manifest.permission.ACCESS_FINE_LOCATION),
            requiredMeasurementPermissions(request(includesGps = true, activityRecognition = true), sdkInt = 28),
        )
    }

    private fun request(includesGps: Boolean, activityRecognition: Boolean) = MeasurementRequest(
        sensorIds = emptySet(),
        includesGps = includesGps,
        samplingPeriodIndex = 0,
        stopOnLowBattery = false,
        useWakeLock = false,
        gpsIntervalSeconds = 1,
        gpsMinDistanceMeters = 0,
        activityRecognition = activityRecognition,
    )
}
