package com.tomasrepcik.sensorbox.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.net.toUri
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.recording.archive.RecordingArchiveSelection

@Composable
fun rememberRecordingArchivePicker(
    onResult: (RecordingArchiveSelection) -> Unit,
    onFailure: (AppError) -> Unit,
): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    val currentOnFailure = rememberUpdatedState(onFailure)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val selection = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                RecordingArchiveSelection.Selected(uri.toString(), result.data?.flags ?: 0)
            } ?: RecordingArchiveSelection.Cancelled
        } else {
            RecordingArchiveSelection.Cancelled
        }
        currentOnResult.value(selection)
    }
    return {
        appResult(AppErrorCode.EXTERNAL_ACTION, "Open recording archive picker") {
            launcher.launch(recordingArchiveIntent())
        }.onFailure(currentOnFailure.value)
    }
}

@Composable
fun rememberPermissionRequest(onResult: () -> Unit, onFailure: (AppError) -> Unit): (Set<String>) -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    val currentOnFailure = rememberUpdatedState(onFailure)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        currentOnResult.value()
    }
    return { permissions ->
        appResult(AppErrorCode.PERMISSION, "Request app permissions") {
            launcher.launch(permissions.toTypedArray())
        }.onFailure(currentOnFailure.value)
    }
}

@Composable
fun rememberLocationPreviewPermissionRequest(onResult: () -> Unit, onFailure: (AppError) -> Unit): () -> Unit {
    val currentOnResult = rememberUpdatedState(onResult)
    val currentOnFailure = rememberUpdatedState(onFailure)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        currentOnResult.value()
    }
    return {
        appResult(AppErrorCode.PERMISSION, "Request location preview permission") {
            launcher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }.onFailure(currentOnFailure.value)
    }
}

fun openWebPage(context: Context, url: String): AppResult<Unit> =
    launchExternalIntent(context, Intent(Intent.ACTION_VIEW, url.toUri()), "Open web page")

@SuppressLint("BatteryLife")
fun requestBatteryOptimizationExemption(context: Context): AppResult<Unit> {
    val request = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        "package:${context.packageName}".toUri(),
    )
    return launchExternalIntent(context, request, "Request battery optimization exemption").fold(
        onSuccess = { AppResult.success(Unit) },
        onFailure = {
            launchExternalIntent(
                context,
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                "Open battery optimization settings",
            )
        },
    )
}

fun shareDiagnosticsText(context: Context, text: String): AppResult<Unit> {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.diagnostics_title))
        .putExtra(Intent.EXTRA_TEXT, text)
    return launchShareIntent(context, intent)
}

fun shareDiagnosticsFile(
    context: Context,
    contentUri: String,
    displayName: String,
    mimeType: String,
): AppResult<Unit> {
    val uri = contentUri.toUri()
    val intent = Intent(Intent.ACTION_SEND)
        .setType(mimeType)
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.diagnostics_title))
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.clipData = ClipData.newUri(context.contentResolver, displayName, uri)
    return launchShareIntent(context, intent)
}

fun copyDiagnostics(context: Context, text: String): AppResult<Unit> = appResult(
    AppErrorCode.EXTERNAL_ACTION,
    "Copy diagnostics",
) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(
        ClipData.newPlainText(context.getString(R.string.diagnostics_title), text),
    )
    Toast.makeText(context, R.string.diagnostics_copied, Toast.LENGTH_LONG).show()
}

fun showDiagnosticsCleared(context: Context) {
    Toast.makeText(context, R.string.diagnostics_cleared, Toast.LENGTH_LONG).show()
}

private fun launchShareIntent(context: Context, intent: Intent): AppResult<Unit> = launchExternalIntent(
    context,
    Intent.createChooser(intent, context.getString(R.string.diagnostics_share)),
    "Share diagnostics",
)

private fun launchExternalIntent(context: Context, intent: Intent, operation: String): AppResult<Unit> = appResult(
    AppErrorCode.EXTERNAL_ACTION,
    operation,
) {
    context.startActivity(intent)
}

private fun recordingArchiveIntent() = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
}
