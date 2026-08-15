package com.motionapps.sensorbox.activities

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.core.error.AppDiagnostics
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.presentation.main.MainEffect
import com.motionapps.sensorbox.presentation.main.MainViewModel
import com.motionapps.sensorbox.presentation.main.SensorBoxApp
import com.motionapps.sensorbox.ui.theme.SensorBoxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val directoryPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.handleStorageResult(it.data)
    }
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.handlePermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_VIEW_PERMISSION_USAGE) viewModel.showPrivacyRationale()
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(viewModel) { viewModel.effects.collect(::handleEffect) }
            SensorBoxTheme {
                SensorBoxApp(state = state, onIntent = viewModel::accept)
            }
        }
    }

    private fun handleEffect(effect: MainEffect) {
        when (effect) {
            MainEffect.PickStorageDirectory -> appResult(AppError.Kind.EXTERNAL_ACTION, "Open storage picker") {
                directoryPicker.launch(storageIntent())
            }

            MainEffect.OpenPrivacyPolicy -> openWebPage(getString(R.string.link_privacy_policy))

            MainEffect.OpenTermsOfUse -> openWebPage(getString(R.string.link_terms))

            MainEffect.RequestBatteryOptimizationExemption -> requestBatteryOptimizationExemption()

            MainEffect.ShareDiagnosticsText -> shareDiagnosticsText()

            MainEffect.ShareDiagnosticsFile -> shareDiagnosticsFile()

            is MainEffect.RequestPermissions -> appResult(AppError.Kind.PERMISSION, "Request app permissions") {
                permissionRequest.launch(effect.permissions.toTypedArray())
            }
        }
    }

    private fun openWebPage(url: String) {
        launchExternalIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "Open web page")
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName"),
        )
        launchExternalIntent(intent, "Request battery optimization exemption").onFailure {
            launchExternalIntent(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                "Open battery optimization settings",
            )
        }
    }

    private fun shareDiagnosticsText() {
        AppDiagnostics.readText().fold(
            onSuccess = { diagnostics ->
                val intent = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_title))
                    .putExtra(Intent.EXTRA_TEXT, diagnostics)
                launchShareIntent(intent).onFailure { showDiagnosticsShareFailure() }
            },
            onFailure = { showDiagnosticsShareFailure() },
        )
    }

    private fun shareDiagnosticsFile() {
        AppDiagnostics.exportFile().fold(
            onSuccess = { file ->
                appResult(AppError.Kind.EXTERNAL_ACTION, "Prepare diagnostics file") {
                    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_title))
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.clipData = ClipData.newUri(contentResolver, file.name, uri)
                    intent
                }.flatMap(::launchShareIntent).onFailure { showDiagnosticsShareFailure() }
            },
            onFailure = { showDiagnosticsShareFailure() },
        )
    }

    private fun launchShareIntent(intent: Intent): Result<Unit> =
        launchExternalIntent(Intent.createChooser(intent, getString(R.string.diagnostics_share)), "Share diagnostics")

    private fun launchExternalIntent(intent: Intent, operation: String): Result<Unit> = appResult(
        AppError.Kind.EXTERNAL_ACTION,
        operation,
    ) {
        startActivity(intent)
    }

    private fun showDiagnosticsShareFailure() {
        Toast.makeText(this, R.string.diagnostics_share_failed, Toast.LENGTH_LONG).show()
    }

    private fun storageIntent() = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }
}
