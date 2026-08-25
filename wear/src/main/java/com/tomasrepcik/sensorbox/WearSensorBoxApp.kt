package com.tomasrepcik.sensorbox

import android.app.Application
import com.tomasrepcik.sensorbox.communication.WearRecordingSessionObserver
import com.tomasrepcik.sensorbox.core.error.FileDiagnostics
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WearSensorBoxApp : Application() {
    @Inject
    lateinit var diagnostics: FileDiagnostics

    @Inject
    lateinit var recordingSessionObserver: WearRecordingSessionObserver

    override fun onCreate() {
        super.onCreate()
        diagnostics.installUncaughtExceptionHandler()
        recordingSessionObserver.start()
    }
}
