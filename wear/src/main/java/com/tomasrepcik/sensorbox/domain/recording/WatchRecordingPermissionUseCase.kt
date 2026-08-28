package com.tomasrepcik.sensorbox.domain.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WatchRecordingPermissionUseCase @Inject constructor(@ApplicationContext private val context: Context) {
    operator fun invoke(includesGps: Boolean): Set<String> =
        requiredWatchRecordingPermissions(includesGps).filterNot(::isGranted).toSet()

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

internal fun requiredWatchRecordingPermissions(includesGps: Boolean): Set<String> = buildSet {
    if (includesGps) add(Manifest.permission.ACCESS_FINE_LOCATION)
}
