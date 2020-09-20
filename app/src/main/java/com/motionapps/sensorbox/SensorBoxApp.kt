package com.motionapps.sensorbox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
/**
 * building block for the Hilt dependency injection framework
 *
 */
class SensorBoxApp: Application()