package com.motionapps.sensorbox

import android.app.Application
import com.motionapps.sensorbox.core.error.FileDiagnostics
import com.motionapps.sensorbox.domain.paired.PhoneRecordingSessionObserver
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

    override fun onCreate() {
        super.onCreate()
        diagnostics.installUncaughtExceptionHandler()
        recordingSessionObserver.start()
    }
}
