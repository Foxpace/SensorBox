package com.tomasrepcik.sensorbox.recording

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetup
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

fun interface RecordingPermissionsUseCase {
    fun missingPermissions(request: RecordingSetup): Set<String>
}

class RecordingPermissionUseCase @Inject constructor(@ApplicationContext private val context: Context) :
    RecordingPermissionsUseCase {
    override fun missingPermissions(request: RecordingSetup): Set<String> =
        requiredRecordingPermissions(request, Build.VERSION.SDK_INT).filterNot(::isGranted).toSet()

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("InlinedApi")
internal fun requiredRecordingPermissions(request: RecordingSetup, sdkInt: Int): Set<String> = buildSet {
    if (sdkInt >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    if (request.includesGps) add(Manifest.permission.ACCESS_FINE_LOCATION)
    if (request.activityRecognition && sdkInt >= 29) add(Manifest.permission.ACTIVITY_RECOGNITION)
}
