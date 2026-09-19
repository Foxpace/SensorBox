package com.tomasrepcik.sensorbox.settings

import com.tomasrepcik.sensorbox.about.OpenSourceLicense
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.core.preferences.AppThemeMode
import com.tomasrepcik.sensorbox.navigation.MainRoute

data class SettingsState(
    val preferences: AppPreferences = AppPreferences(),
    val diagnosticsText: String? = null,
    val diagnosticsLoaded: Boolean = false,
    val isBatteryOptimizationExempt: Boolean = false,
    val openSourceLicenses: List<OpenSourceLicense> = emptyList(),
    val selectedLicenseName: String? = null,
)

sealed interface SettingsIntent {
    data class SetSamplingPeriod(val index: Int) : SettingsIntent
    data class SetStopOnLowBattery(val enabled: Boolean) : SettingsIntent
    data class SetWakeLock(val enabled: Boolean) : SettingsIntent
    data class SetKeepScreenAwake(val enabled: Boolean) : SettingsIntent
    data class SetGpsInterval(val seconds: Int) : SettingsIntent
    data class SetGpsDistance(val meters: Int) : SettingsIntent
    data class SetThemeMode(val mode: AppThemeMode) : SettingsIntent
    data class SetDynamicColors(val enabled: Boolean) : SettingsIntent
    data object RequestBatteryOptimizationExemption : SettingsIntent
    data object ShareDiagnosticsText : SettingsIntent
    data object ShareDiagnosticsFile : SettingsIntent
    data object ViewDiagnostics : SettingsIntent
    data object CopyDiagnostics : SettingsIntent
    data object ClearDiagnostics : SettingsIntent
    data object DismissDiagnostics : SettingsIntent
    data object RefreshBatteryOptimization : SettingsIntent
    data class SelectOpenSourceLicense(val name: String) : SettingsIntent
    data object DismissOpenSourceLicense : SettingsIntent
    data class Navigate(val route: MainRoute) : SettingsIntent
}

sealed interface SettingsEffect {
    data object RequestBatteryOptimizationExemption : SettingsEffect
    data class ShareDiagnosticsText(val text: String) : SettingsEffect
    data class ShareDiagnosticsFile(val contentUri: String, val displayName: String, val mimeType: String) :
        SettingsEffect
    data class CopyDiagnosticsText(val text: String) : SettingsEffect
    data object DiagnosticsCleared : SettingsEffect
    data class Navigate(val route: MainRoute) : SettingsEffect
}
