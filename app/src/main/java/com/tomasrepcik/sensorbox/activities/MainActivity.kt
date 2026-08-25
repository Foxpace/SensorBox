package com.tomasrepcik.sensorbox.activities

import android.annotation.SuppressLint
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticsStore
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.presentation.main.MainViewModel
import com.tomasrepcik.sensorbox.presentation.main.OnboardingEffect
import com.tomasrepcik.sensorbox.presentation.main.OnboardingViewModel
import com.tomasrepcik.sensorbox.presentation.main.RecordingEffect
import com.tomasrepcik.sensorbox.presentation.main.RecordingViewModel
import com.tomasrepcik.sensorbox.presentation.main.SensorBoxApp
import com.tomasrepcik.sensorbox.presentation.main.SettingsEffect
import com.tomasrepcik.sensorbox.presentation.main.SettingsViewModel
import com.tomasrepcik.sensorbox.ui.theme.SensorBoxTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var diagnosticsStore: DiagnosticsStore

    private val mainViewModel: MainViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()
    private val recordingViewModel: RecordingViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private var storageRequestOwner = StorageRequestOwner.RECORDING
    private val directoryPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when (storageRequestOwner) {
            StorageRequestOwner.ONBOARDING -> onboardingViewModel.handleStorageResult(it.data)
            StorageRequestOwner.RECORDING -> recordingViewModel.handleStorageResult(it.data)
        }
    }
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        recordingViewModel.handlePermissionResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_VIEW_PERMISSION_USAGE) mainViewModel.showPrivacyRationale()
        setContent {
            val mainState by mainViewModel.state.collectAsStateWithLifecycle()
            val onboardingState by onboardingViewModel.state.collectAsStateWithLifecycle()
            val recordingState by recordingViewModel.state.collectAsStateWithLifecycle()
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
            SensorBoxTheme(
                themeMode = settingsState.preferences.display.themeMode,
                dynamicColor = settingsState.preferences.display.dynamicColors,
            ) {
                SensorBoxApp(
                    mainState = mainState,
                    onboardingState = onboardingState,
                    recordingState = recordingState,
                    settingsState = settingsState,
                    onNavigate = mainViewModel::navigate,
                    onOnboardingIntent = onboardingViewModel::accept,
                    onRecordingIntent = recordingViewModel::accept,
                    onSettingsIntent = settingsViewModel::accept,
                )
            }
        }
    }

    private fun handleOnboardingEffect(effect: OnboardingEffect) {
        when (effect) {
            OnboardingEffect.PickStorageDirectory -> openStoragePicker(StorageRequestOwner.ONBOARDING)
            OnboardingEffect.OpenPrivacyPolicy -> openWebPage(getString(R.string.link_privacy_policy))
            OnboardingEffect.OpenTermsOfUse -> openWebPage(getString(R.string.link_terms))
            OnboardingEffect.RequestBatteryOptimizationExemption -> requestBatteryOptimizationExemption()
            is OnboardingEffect.Navigate -> mainViewModel.navigate(effect.route)
        }
    }

    private fun handleRecordingEffect(effect: RecordingEffect) {
        when (effect) {
            RecordingEffect.PickStorageDirectory -> openStoragePicker(StorageRequestOwner.RECORDING)

            is RecordingEffect.Navigate -> mainViewModel.navigate(effect.route)

            is RecordingEffect.RequestPermissions -> appResult(AppErrorCode.PERMISSION, "Request app permissions") {
                permissionRequest.launch(effect.permissions.toTypedArray())
            }
        }
    }

    private fun handleSettingsEffect(effect: SettingsEffect) {
        when (effect) {
            SettingsEffect.RequestBatteryOptimizationExemption -> requestBatteryOptimizationExemption()
            SettingsEffect.ShareDiagnosticsText -> shareDiagnosticsText()
            SettingsEffect.ShareDiagnosticsFile -> shareDiagnosticsFile()
            is SettingsEffect.CopyDiagnosticsText -> copyDiagnostics(effect.text)
            SettingsEffect.DiagnosticsCleared -> showToast(R.string.diagnostics_cleared)
            is SettingsEffect.DiagnosticsFailed -> showToast(R.string.diagnostics_action_failed)
            is SettingsEffect.Navigate -> mainViewModel.navigate(effect.route)
        }
    }

    private fun openStoragePicker(owner: StorageRequestOwner) {
        storageRequestOwner = owner
        appResult(AppErrorCode.EXTERNAL_ACTION, "Open storage picker") {
            directoryPicker.launch(storageIntent())
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
        diagnosticsStore.readText().fold(
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
        diagnosticsStore.exportFile().fold(
            onSuccess = { file ->
                appResult(AppErrorCode.EXTERNAL_ACTION, "Prepare diagnostics file") {
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

    private fun launchShareIntent(intent: Intent): AppResult<Unit> =
        launchExternalIntent(Intent.createChooser(intent, getString(R.string.diagnostics_share)), "Share diagnostics")

    private fun launchExternalIntent(intent: Intent, operation: String): AppResult<Unit> = appResult(
        AppErrorCode.EXTERNAL_ACTION,
        operation,
    ) {
        startActivity(intent)
    }

    private fun showDiagnosticsShareFailure() {
        showToast(R.string.diagnostics_share_failed)
    }

    private fun copyDiagnostics(text: String) {
        appResult(AppErrorCode.EXTERNAL_ACTION, "Copy diagnostics") {
            getSystemService(ClipboardManager::class.java).setPrimaryClip(
                ClipData.newPlainText(getString(R.string.diagnostics_title), text),
            )
        }.fold(
            onSuccess = { showToast(R.string.diagnostics_copied) },
            onFailure = { showToast(R.string.diagnostics_action_failed) },
        )
    }

    private fun showToast(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun storageIntent() = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }

    private enum class StorageRequestOwner {
        ONBOARDING,
        RECORDING,
    }
}
