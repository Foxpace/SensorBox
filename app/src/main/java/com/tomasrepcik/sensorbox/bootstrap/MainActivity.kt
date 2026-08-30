package com.tomasrepcik.sensorbox.bootstrap

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.appResult
import com.tomasrepcik.sensorbox.design.SensorBoxTheme
import com.tomasrepcik.sensorbox.measurements.browser.MeasurementBrowserEffect
import com.tomasrepcik.sensorbox.measurements.browser.MeasurementBrowserViewModel
import com.tomasrepcik.sensorbox.navigation.MainViewModel
import com.tomasrepcik.sensorbox.navigation.SensorBoxApp
import com.tomasrepcik.sensorbox.onboarding.OnboardingEffect
import com.tomasrepcik.sensorbox.onboarding.OnboardingViewModel
import com.tomasrepcik.sensorbox.recording.RecordingEffect
import com.tomasrepcik.sensorbox.recording.RecordingViewModel
import com.tomasrepcik.sensorbox.recording.archive.RecordingArchiveSelection
import com.tomasrepcik.sensorbox.settings.SettingsEffect
import com.tomasrepcik.sensorbox.settings.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val recordingViewModel: RecordingViewModel by viewModels()
    private val measurementBrowserViewModel: MeasurementBrowserViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var recordingArchiveRequestOwner = RecordingArchiveRequestOwner.RECORDING
    private val directoryPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val selection = if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                RecordingArchiveSelection.Selected(uri.toString(), it.data?.flags ?: 0)
            } ?: RecordingArchiveSelection.Cancelled
        } else {
            RecordingArchiveSelection.Cancelled
        }
        when (recordingArchiveRequestOwner) {
            RecordingArchiveRequestOwner.ONBOARDING -> onboardingViewModel.handleRecordingArchiveResult(selection)
            RecordingArchiveRequestOwner.RECORDING -> recordingViewModel.handleRecordingArchiveResult(selection)
        }
    }
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        recordingViewModel.handlePermissionResult()
    }
    private val locationPreviewPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            recordingViewModel.handleLocationPreviewPermissionResult()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_VIEW_PERMISSION_USAGE) mainViewModel.showPrivacyRationale()
        setContent {
            val mainState by mainViewModel.state.collectAsStateWithLifecycle()
            val onboardingState by onboardingViewModel.state.collectAsStateWithLifecycle()
            val recordingState by recordingViewModel.state.collectAsStateWithLifecycle()
            val measurementBrowserState by measurementBrowserViewModel.state.collectAsStateWithLifecycle()
            val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(onboardingViewModel) {
                onboardingViewModel.effects.collect(::handleOnboardingEffect)
            }
            LaunchedEffect(recordingViewModel) {
                recordingViewModel.effects.collect(::handleRecordingEffect)
            }
            LaunchedEffect(settingsViewModel) {
                settingsViewModel.effects.collect(::handleSettingsEffect)
            }
            LaunchedEffect(measurementBrowserViewModel) {
                measurementBrowserViewModel.effects.collect(::handleMeasurementBrowserEffect)
            }
            SensorBoxTheme(
                themeMode = settingsState.preferences.display.themeMode,
                dynamicColor = settingsState.preferences.display.dynamicColors,
            ) {
                SensorBoxApp(
                    mainState = mainState,
                    onboardingState = onboardingState,
                    recordingState = recordingState,
                    measurementBrowserState = measurementBrowserState,
                    settingsState = settingsState,
                    onNavigate = mainViewModel::navigate,
                    onOnboardingIntent = onboardingViewModel::accept,
                    onRecordingIntent = recordingViewModel::accept,
                    onMeasurementBrowserIntent = measurementBrowserViewModel::onIntent,
                    onSettingsIntent = settingsViewModel::accept,
                    onDismissFailure = mainViewModel::dismissFailure,
                )
            }
        }
    }

    private fun handleOnboardingEffect(effect: OnboardingEffect) {
        when (effect) {
            OnboardingEffect.PickRecordingArchive -> openRecordingArchivePicker(RecordingArchiveRequestOwner.ONBOARDING)

            OnboardingEffect.OpenPrivacyPolicy -> openWebPage(getString(R.string.link_privacy_policy))

            OnboardingEffect.OpenTermsOfUse -> openWebPage(getString(R.string.link_terms))

            OnboardingEffect.RequestBatteryOptimizationExemption ->
                requestBatteryOptimizationExemption(onboardingViewModel::reportFailure)

            is OnboardingEffect.Navigate -> {
                recordingViewModel.refreshRecordingArchive()
                mainViewModel.navigate(effect.route)
            }
        }
    }

    private fun handleRecordingEffect(effect: RecordingEffect) {
        when (effect) {
            RecordingEffect.PickRecordingArchive -> openRecordingArchivePicker(RecordingArchiveRequestOwner.RECORDING)

            is RecordingEffect.Navigate -> mainViewModel.navigate(effect.route)

            is RecordingEffect.RequestPermissions -> appResult(AppErrorCode.PERMISSION, "Request app permissions") {
                permissionRequest.launch(effect.permissions.toTypedArray())
            }.onFailure(recordingViewModel::reportFailure)

            RecordingEffect.RequestLocationPreviewPermission -> appResult(
                AppErrorCode.PERMISSION,
                "Request location preview permission",
            ) {
                locationPreviewPermissionRequest.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }.onFailure(recordingViewModel::reportFailure)
        }
    }

    private fun handleSettingsEffect(effect: SettingsEffect) {
        when (effect) {
            SettingsEffect.RequestBatteryOptimizationExemption ->
                requestBatteryOptimizationExemption(settingsViewModel::reportFailure)

            is SettingsEffect.ShareDiagnosticsText -> shareDiagnosticsText(effect.text)

            is SettingsEffect.ShareDiagnosticsFile -> shareDiagnosticsFile(effect)

            is SettingsEffect.CopyDiagnosticsText -> copyDiagnostics(effect.text)

            SettingsEffect.DiagnosticsCleared -> showToast(R.string.diagnostics_cleared)

            is SettingsEffect.Navigate -> mainViewModel.navigate(effect.route)
        }
    }

    private fun handleMeasurementBrowserEffect(effect: MeasurementBrowserEffect) {
        when (effect) {
            is MeasurementBrowserEffect.Navigate -> mainViewModel.navigate(effect.route)
        }
    }

    private fun openRecordingArchivePicker(owner: RecordingArchiveRequestOwner) {
        recordingArchiveRequestOwner = owner
        appResult(AppErrorCode.EXTERNAL_ACTION, "Open recording archive picker") {
            directoryPicker.launch(recordingArchiveIntent())
        }.onFailure(::reportRecordingArchivePickerFailure)
    }

    private fun openWebPage(url: String) {
        launchExternalIntent(Intent(Intent.ACTION_VIEW, Uri.parse(url)), "Open web page")
            .onFailure(onboardingViewModel::reportFailure)
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption(
        reportFailure: (com.tomasrepcik.sensorbox.core.failure.AppError) -> Unit,
    ) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:$packageName"),
        )
        launchExternalIntent(intent, "Request battery optimization exemption").onFailure {
            launchExternalIntent(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                "Open battery optimization settings",
            ).onFailure(reportFailure)
        }
    }

    private fun shareDiagnosticsText(text: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_title))
            .putExtra(Intent.EXTRA_TEXT, text)
        launchShareIntent(intent).onFailure(settingsViewModel::reportFailure)
    }

    private fun shareDiagnosticsFile(effect: SettingsEffect.ShareDiagnosticsFile) {
        val uri = Uri.parse(effect.contentUri)
        val intent = Intent(Intent.ACTION_SEND)
            .setType(effect.mimeType)
            .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_title))
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newUri(contentResolver, effect.displayName, uri)
        launchShareIntent(intent).onFailure(settingsViewModel::reportFailure)
    }

    private fun launchShareIntent(intent: Intent): AppResult<Unit> =
        launchExternalIntent(Intent.createChooser(intent, getString(R.string.diagnostics_share)), "Share diagnostics")

    private fun launchExternalIntent(intent: Intent, operation: String): AppResult<Unit> = appResult(
        AppErrorCode.EXTERNAL_ACTION,
        operation,
    ) {
        startActivity(intent)
    }

    private fun copyDiagnostics(text: String) {
        appResult(AppErrorCode.EXTERNAL_ACTION, "Copy diagnostics") {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(
                ClipData.newPlainText(getString(R.string.diagnostics_title), text),
            )
        }.fold(
            onSuccess = { showToast(R.string.diagnostics_copied) },
            onFailure = settingsViewModel::reportFailure,
        )
    }

    private fun reportRecordingArchivePickerFailure(error: com.tomasrepcik.sensorbox.core.failure.AppError) {
        when (recordingArchiveRequestOwner) {
            RecordingArchiveRequestOwner.ONBOARDING -> onboardingViewModel.reportFailure(error)
            RecordingArchiveRequestOwner.RECORDING -> recordingViewModel.reportFailure(error)
        }
    }

    private fun showToast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun recordingArchiveIntent() = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }

    private enum class RecordingArchiveRequestOwner {
        ONBOARDING,
        RECORDING,
    }
}
