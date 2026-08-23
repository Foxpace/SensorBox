package com.motionapps.sensorbox.domain.measurement

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthPermissions
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MeasurementPermissionUseCase @Inject constructor(@ApplicationContext private val context: Context) {
    fun missingPermissions(request: MeasurementRequest, includesHeartRate: Boolean): Set<String> = buildSet {
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        if (request.includesGps) add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (request.activityRecognition && Build.VERSION.SDK_INT >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (includesHeartRate) addHeartRatePermissions()
    }.filterNot(::isGranted).toSet()

    private fun MutableSet<String>.addHeartRatePermissions() {
        when {
            Build.VERSION.SDK_INT >= 36 -> {
                add(HealthPermissions.READ_HEART_RATE)
                add(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND)
            }

            Build.VERSION.SDK_INT >= 33 -> {
                add(Manifest.permission.BODY_SENSORS)
                add(Manifest.permission.BODY_SENSORS_BACKGROUND)
            }

            else -> add(Manifest.permission.BODY_SENSORS)
        }
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
