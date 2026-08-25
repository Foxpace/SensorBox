package com.motionapps.sensorbox.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.DiagnosticsStore
import com.motionapps.sensorbox.core.preferences.AppPreferencesIntent
import com.motionapps.sensorbox.core.preferences.AppPreferencesRepository
import com.motionapps.sensorbox.di.IoDispatcher
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
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    private val mutableEffects = Channel<SettingsEffect>(Channel.BUFFERED)

    val state: StateFlow<SettingsState> = mutableState.asStateFlow()
    val effects = mutableEffects.receiveAsFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { result ->
                when (result) {
                    is AppResult.Success -> mutableState.value = state.value.copy(
                        preferences = result.value,
                        errorCode = null,
                    )

                    is AppResult.Failure -> mutableState.value = state.value.copy(errorCode = result.error.code)
                }
            }
        }
    }

    fun accept(intent: SettingsIntent) {
        intent.toPreferencesIntent()?.let {
            update(it)
            return
        }
        when (intent) {
            SettingsIntent.RequestBatteryOptimizationExemption ->
                mutableEffects.trySend(SettingsEffect.RequestBatteryOptimizationExemption)

            SettingsIntent.ShareDiagnosticsText -> shareDiagnosticsText()

            SettingsIntent.ShareDiagnosticsFile -> shareDiagnosticsFile()

            SettingsIntent.ViewDiagnostics -> viewDiagnostics()

            SettingsIntent.CopyDiagnostics -> copyDiagnostics()

            SettingsIntent.ClearDiagnostics -> clearDiagnostics()

            SettingsIntent.DismissDiagnostics -> mutableState.value = state.value.copy(diagnosticsText = null)

            is SettingsIntent.Navigate -> mutableEffects.trySend(SettingsEffect.Navigate(intent.route))

            is SettingsIntent.SetSamplingPeriod,
            is SettingsIntent.SetLowBatteryRestriction,
            is SettingsIntent.SetWakeLock,
            is SettingsIntent.SetKeepScreenAwake,
            is SettingsIntent.SetGpsInterval,
            is SettingsIntent.SetGpsDistance,
            is SettingsIntent.SetThemeMode,
            is SettingsIntent.SetDynamicColors,
            -> Unit
        }
    }

    private fun SettingsIntent.toPreferencesIntent(): AppPreferencesIntent? = when (this) {
        is SettingsIntent.SetSamplingPeriod -> AppPreferencesIntent.SetSensorSamplingPeriod(index)
        is SettingsIntent.SetLowBatteryRestriction -> AppPreferencesIntent.SetLowBatteryRestriction(enabled)
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
                mutableState.value = state.value.copy(errorCode = error.code)
            }
        }
    }

    private fun viewDiagnostics() {
        viewModelScope.launch(ioDispatcher) {
            when (val result = diagnosticsStore.readText()) {
                is AppResult.Success -> mutableState.value = state.value.copy(
                    diagnosticsText = result.value,
                    diagnosticsLoaded = true,
                    errorCode = null,
                )

                is AppResult.Failure -> fail(result.error.code)
            }
        }
    }

    private fun shareDiagnosticsText() {
        withDiagnostics { mutableEffects.send(SettingsEffect.ShareDiagnosticsText) }
    }

    private fun shareDiagnosticsFile() {
        withDiagnostics { mutableEffects.send(SettingsEffect.ShareDiagnosticsFile) }
    }

    private fun withDiagnostics(action: suspend () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            when (val result = diagnosticsStore.readText()) {
                is AppResult.Success -> {
                    mutableState.value = state.value.copy(
                        diagnosticsText = result.value,
                        diagnosticsLoaded = true,
                        errorCode = null,
                    )
                    if (result.value.isNotBlank()) action()
                }

                is AppResult.Failure -> fail(result.error.code)
            }
        }
    }

    private fun copyDiagnostics() {
        viewModelScope.launch(ioDispatcher) {
            when (val result = diagnosticsStore.readText()) {
                is AppResult.Success -> mutableEffects.send(SettingsEffect.CopyDiagnosticsText(result.value))
                is AppResult.Failure -> fail(result.error.code)
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
                        errorCode = null,
                    )
                    mutableEffects.send(SettingsEffect.DiagnosticsCleared)
                }

                is AppResult.Failure -> fail(result.error.code)
            }
        }
    }

    private suspend fun fail(code: AppErrorCode) {
        mutableState.value = state.value.copy(errorCode = code)
        mutableEffects.send(SettingsEffect.DiagnosticsFailed(code))
    }
}
