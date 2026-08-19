package com.motionapps.sensorbox

import android.app.Application
import com.motionapps.sensorbox.communication.WearRecordingSessionObserver
import com.motionapps.sensorbox.core.error.FileDiagnostics
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
