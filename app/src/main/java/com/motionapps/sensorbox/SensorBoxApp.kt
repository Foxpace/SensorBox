package com.motionapps.sensorbox

import android.app.Application
import com.motionapps.sensorbox.core.error.AppDiagnostics
import dagger.hilt.android.HiltAndroidApp

/**
 * building block for the Hilt dependency injection framework
 *
 */
@HiltAndroidApp
class SensorBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDiagnostics.install(this)
    }
}
