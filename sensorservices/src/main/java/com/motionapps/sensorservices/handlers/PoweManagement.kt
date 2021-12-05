package com.motionapps.sensorservices.handlers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.motionapps.sensorservices.R
import es.dmoral.toasty.Toasty

object PowerManagement {

    /**
     * request to add app to whitelist
     *
     */
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("BatteryLife")
    fun tryToIgnoreBatteryOptimisations(context: Context, withIcon: Boolean = true) {

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        if (pm == null) {
            Toasty.error(
                context,
                context.getString(R.string.intro_ignored_optimisations_error),
                Toasty.LENGTH_LONG,
                withIcon
            ).show()
            return
        }

        if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
            Toasty.success(
                context,
                context.getString(R.string.intro_ignored_optimisations),
                Toasty.LENGTH_LONG,
                withIcon
            ).show()
        }

        val intent = Intent()
        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        intent.data = Uri.parse("package:" + context.packageName)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toasty.error(
                context,
                context.getString(R.string.intro_ignored_optimisations_error),
                Toasty.LENGTH_LONG,
                withIcon
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isOptimised(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager? ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
