package com.motionapps.sensorbox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * building block for the Hilt dependency injection framework
 *
 */
@HiltAndroidApp
class SensorBoxApp: Application()