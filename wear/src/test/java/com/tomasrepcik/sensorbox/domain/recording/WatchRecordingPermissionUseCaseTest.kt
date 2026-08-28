package com.tomasrepcik.sensorbox.domain.recording

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchRecordingPermissionUseCaseTest {
    @Test
    fun `Given watch GPS When permissions are derived Then only location is required`() {
        assertEquals(
            setOf(Manifest.permission.ACCESS_FINE_LOCATION),
            requiredWatchRecordingPermissions(includesGps = true),
        )
    }

    @Test
    fun `Given watch sensors without GPS When permissions are derived Then none are required`() {
        assertEquals(
            emptySet<String>(),
            requiredWatchRecordingPermissions(includesGps = false),
        )
    }
}
