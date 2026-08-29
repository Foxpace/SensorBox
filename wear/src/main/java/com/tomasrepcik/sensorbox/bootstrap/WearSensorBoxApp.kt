package com.tomasrepcik.sensorbox.bootstrap

import android.app.Application
import com.tomasrepcik.sensorbox.core.diagnostics.FileDiagnostics
import com.tomasrepcik.sensorbox.pairedrecording.WearRecordingSessionObserver
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
