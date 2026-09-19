package com.tomasrepcik.sensorbox.recording

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tomasrepcik.sensorbox.recordinghost.session.RecordingService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordingServiceManifestTest {
    @Test
    fun givenInstalledPhoneAppWhenRecordingStartsThenRecordingServiceIsRegistered() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        val component = ComponentName(context, RecordingService::class.java)

        // When
        @Suppress("DEPRECATION")
        val service = context.packageManager.getServiceInfo(component, PackageManager.GET_META_DATA)

        // Then
        assertEquals(RecordingService::class.java.name, service.name)
    }
}
