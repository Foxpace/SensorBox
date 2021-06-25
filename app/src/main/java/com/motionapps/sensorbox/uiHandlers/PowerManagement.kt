package com.motionapps.sensorbox.uiHandlers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.motionapps.sensorbox.R
import es.dmoral.toasty.Toasty

object PowerManagement {

    /**
     * request to add app to whitelist
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("BatteryLife")
    fun checkOptimisation(activity: Activity) {

        val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager?
        if (pm != null) {
            if (!pm.isIgnoringBatteryOptimizations(activity.packageName)) {
                val intent = Intent()
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:" + activity.packageName)
                try {
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Toasty.error(activity, activity.getString(R.string.intro_ignored_optimisations_error), Toasty.LENGTH_LONG, true).show()
                }
            }else{
                Toasty.success(activity, activity.getString(R.string.intro_ignored_optimisations), Toasty.LENGTH_LONG, true).show()
            }
        }else{
            Toasty.error(activity, activity.getString(R.string.intro_ignored_optimisations_error), Toasty.LENGTH_LONG, true).show()
        }
    }

}