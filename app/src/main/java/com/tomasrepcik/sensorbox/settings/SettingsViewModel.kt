package com.tomasrepcik.sensorbox.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasrepcik.sensorbox.about.OpenSourceLicenseRepository
import com.tomasrepcik.sensorbox.bootstrap.IoDispatcher
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.DiagnosticsStore
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesIntent
import com.tomasrepcik.sensorbox.core.preferences.AppPreferencesRepository
import com.tomasrepcik.sensorbox.diagnostics.DiagnosticsShareFilePreparer
import com.tomasrepcik.sensorbox.recording.preview.DevicePreviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val diagnosticsStore: DiagnosticsStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val appFailures: AppFailureStore,
    private val devicePreview: DevicePreviewRepository,
    private val licenses: OpenSourceLicenseRepository,
    private val diagnosticsShareFile: DiagnosticsShareFilePreparer,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    private val mutableEffects = Channel<SettingsEffect>(Channel.BUFFERED)

    val state: StateFlow<SettingsState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        loadLicenses()
        viewModelScope.launch {
            preferencesRepository.preferences.collect { result ->
                when (result) {
                    is AppResult.Success -> mutableState.value = state.value.copy(
                        preferences = result.value,
                    )

                    is AppResult.Failure -> appFailures.show(result.error)
                }
            }
        }
    }

    fun accept(intent: SettingsIntent) {
        intent.toPreferencesIntent()?.let {
            update(it)
            return
        }
        if (handleStateIntent(intent)) return
        when (intent) {
            SettingsIntent.RequestBatteryOptimizationExemption ->
                mutableEffects.trySend(SettingsEffect.RequestBatteryOptimizationExemption)

            SettingsIntent.ShareDiagnosticsText -> shareDiagnosticsText()

            SettingsIntent.ShareDiagnosticsFile -> shareDiagnosticsFile()

            SettingsIntent.ViewDiagnostics -> viewDiagnostics()

            SettingsIntent.CopyDiagnostics -> copyDiagnostics()

            SettingsIntent.ClearDiagnostics -> clearDiagnostics()

            SettingsIntent.DismissDiagnostics,
            SettingsIntent.RefreshBatteryOptimization,
            is SettingsIntent.SelectOpenSourceLicense,
            SettingsIntent.DismissOpenSourceLicense,
            is SettingsIntent.Navigate,
            is SettingsIntent.SetSamplingPeriod,
            is SettingsIntent.SetStopOnLowBattery,
            is SettingsIntent.SetWakeLock,
            is SettingsIntent.SetKeepScreenAwake,
            is SettingsIntent.SetGpsInterval,
            is SettingsIntent.SetGpsDistance,
            is SettingsIntent.SetThemeMode,
            is SettingsIntent.SetDynamicColors,
            -> Unit
        }
    }

    private fun handleStateIntent(intent: SettingsIntent): Boolean {
        when (intent) {
            SettingsIntent.DismissDiagnostics -> mutableState.value = state.value.copy(diagnosticsText = null)

            SettingsIntent.RefreshBatteryOptimization -> refreshBatteryOptimization()

            is SettingsIntent.SelectOpenSourceLicense ->
                mutableState.value = state.value.copy(selectedLicenseName = intent.name)

            SettingsIntent.DismissOpenSourceLicense ->
                mutableState.value = state.value.copy(selectedLicenseName = null)

            is SettingsIntent.Navigate -> mutableEffects.trySend(SettingsEffect.Navigate(intent.route))

            else -> return false
        }
        return true
    }

    private fun refreshBatteryOptimization() {
        when (val result = devicePreview.batteryOptimizationExemption()) {
            is AppResult.Success -> mutableState.value = state.value.copy(isBatteryOptimizationExempt = result.value)
            is AppResult.Failure -> fail(result.error)
        }
    }

    private fun loadLicenses() {
        viewModelScope.launch(ioDispatcher) {
            when (val result = licenses.load()) {
                is AppResult.Success -> mutableState.value = state.value.copy(openSourceLicenses = result.value)
                is AppResult.Failure -> fail(result.error)
            }
        }
    }

    private fun SettingsIntent.toPreferencesIntent(): AppPreferencesIntent? = when (this) {
        is SettingsIntent.SetSamplingPeriod -> AppPreferencesIntent.SetSensorSamplingPeriod(index)
        is SettingsIntent.SetStopOnLowBattery -> AppPreferencesIntent.SetStopOnLowBattery(enabled)
        is SettingsIntent.SetWakeLock -> AppPreferencesIntent.SetWakeLock(enabled)
        is SettingsIntent.SetKeepScreenAwake -> AppPreferencesIntent.SetKeepPhoneDisplayOn(enabled)
        is SettingsIntent.SetGpsInterval -> AppPreferencesIntent.SetGpsInterval(seconds)
        is SettingsIntent.SetGpsDistance -> AppPreferencesIntent.SetGpsMinDistance(meters)
        is SettingsIntent.SetThemeMode -> AppPreferencesIntent.SetThemeMode(mode)
        is SettingsIntent.SetDynamicColors -> AppPreferencesIntent.SetDynamicColors(enabled)
        else -> null
    }

    private fun update(intent: AppPreferencesIntent) {
        viewModelScope.launch {
            preferencesRepository.dispatch(intent).onFailure { error ->
                appFailures.show(error)
            }
        }
    }

    private fun viewDiagnostics() {
        viewModelScope.launch(ioDispatcher) {
            when (val result = diagnosticsStore.readText()) {
                is AppResult.Success -> mutableState.value = state.value.copy(
                    diagnosticsText = result.value,
                    diagnosticsLoaded = true,
                )

                is AppResult.Failure -> fail(result.error)
            }
        }
    }

    private fun shareDiagnosticsText() {
        withDiagnostics { text -> mutableEffects.send(SettingsEffect.ShareDiagnosticsText(text)) }
    }

    private fun shareDiagnosticsFile() {
        withDiagnostics { text ->
            when (val result = diagnosticsShareFile.prepare(text)) {
                is AppResult.Success -> mutableEffects.send(
                    SettingsEffect.ShareDiagnosticsFile(
                        result.value.contentUri,
                        result.value.displayName,
                        result.value.mimeType,
                    ),
                )

                is AppResult.Failure -> fail(result.error)
            }
        }
    }

    private fun withDiagnostics(operation: suspend (String) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = diagnosticsStore.readText()) {
                is AppResult.Success -> {
                    mutableState.value = state.value.copy(
                        diagnosticsText = result.value,
                        diagnosticsLoaded = true,
                    )
                    if (result.value.isNotBlank()) operation(result.value)
                }

                is AppResult.Failure -> fail(result.error)
            }
        }
    }

    private fun copyDiagnostics() {
        viewModelScope.launch(ioDispatcher) {
            when (val result = diagnosticsStore.readText()) {
                is AppResult.Success -> mutableEffects.send(SettingsEffect.CopyDiagnosticsText(result.value))
                is AppResult.Failure -> fail(result.error)
            }
        }
    }

    private fun clearDiagnostics() {
        viewModelScope.launch(ioDispatcher) {
            when (val result = diagnosticsStore.clear()) {
                is AppResult.Success -> {
                    mutableState.value = state.value.copy(
                        diagnosticsText = "",
                        diagnosticsLoaded = true,
                    )
                    mutableEffects.send(SettingsEffect.DiagnosticsCleared)
                }

                is AppResult.Failure -> fail(result.error)
            }
        }
    }

    private fun fail(error: AppError) {
        appFailures.show(error)
    }

    fun reportFailure(error: AppError) {
        fail(error)
    }
}
