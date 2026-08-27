package com.tomasrepcik.sensorbox.domain.measurement

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

fun interface MeasurementPermissionsUseCase {
    fun missingPermissions(request: MeasurementRequest): Set<String>
}

class MeasurementPermissionUseCase @Inject constructor(@ApplicationContext private val context: Context) :
    MeasurementPermissionsUseCase {
    override fun missingPermissions(request: MeasurementRequest): Set<String> =
        requiredMeasurementPermissions(request, Build.VERSION.SDK_INT).filterNot(::isGranted).toSet()

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

internal fun requiredMeasurementPermissions(request: MeasurementRequest, sdkInt: Int): Set<String> = buildSet {
    if (sdkInt >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    if (request.includesGps) add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (request.activityRecognition && sdkInt >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
}
