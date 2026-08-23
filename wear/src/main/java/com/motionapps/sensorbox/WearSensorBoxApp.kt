package com.motionapps.sensorbox

import android.app.Application
import com.motionapps.sensorbox.core.error.AppDiagnostics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WearSensorBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDiagnostics.install(this)
    }
}
