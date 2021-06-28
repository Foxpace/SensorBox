package com.motionapps.sensorbox

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.multidex.MultiDex
import dagger.hilt.android.HiltAndroidApp

/**
 * building block for the Hilt dependency injection framework
 *
 */
@HiltAndroidApp
class SensorBoxApp: Application(){

    override fun attachBaseContext(context: Context?) {
        super.attachBaseContext(context)
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            MultiDex.install(this)
        }
    }

}