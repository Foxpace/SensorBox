package com.motionapps.sensorbox.domain.measurement

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WearMeasurementPermissionUseCase @Inject constructor(@ApplicationContext private val context: Context) {
    operator fun invoke(includesGps: Boolean): Set<String> =
        requiredWearMeasurementPermissions(includesGps, Build.VERSION.SDK_INT).filterNot(::isGranted).toSet()

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

internal fun requiredWearMeasurementPermissions(includesGps: Boolean, sdkInt: Int): Set<String> = buildSet {
    if (sdkInt >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    if (includesGps) add(Manifest.permission.ACCESS_FINE_LOCATION)
}
