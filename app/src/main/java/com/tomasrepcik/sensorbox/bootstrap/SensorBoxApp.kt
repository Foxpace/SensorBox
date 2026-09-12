package com.tomasrepcik.sensorbox.bootstrap

import android.app.Application
import com.tomasrepcik.sensorbox.core.diagnostics.FileDiagnostics
import com.tomasrepcik.sensorbox.pairedrecording.PhoneRecordingSessionObserver
import com.tomasrepcik.sensorbox.recording.sources.RecordingSourceAvailability
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * building block for the Hilt dependency injection framework
 *
 */
@HiltAndroidApp
class SensorBoxApp : Application() {
    @Inject
    lateinit var diagnostics: FileDiagnostics

    @Inject
    lateinit var recordingSessionObserver: PhoneRecordingSessionObserver

    @Inject
    lateinit var recordingSourceAvailability: RecordingSourceAvailability

    override fun onCreate() {
        super.onCreate()
        diagnostics.installUncaughtExceptionHandler()
        recordingSessionObserver.start()
        recordingSourceAvailability.start()
    }
}
