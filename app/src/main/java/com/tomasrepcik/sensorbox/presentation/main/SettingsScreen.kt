package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.tomasrepcik.sensorbox.R

@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { onIntent(SettingsIntent.Navigate(MainRoute.RECORD)) },
) {
    LaunchedEffect(Unit) { onIntent(SettingsIntent.ViewDiagnostics) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onIntent(SettingsIntent.RefreshBatteryOptimization) }
    SensorBoxBackScreen(
        title = stringResource(R.string.recording_settings),
        onBack = onBack,
        modifier = modifier,
        itemSpacing = 0.dp,
    ) {
        appearanceSettingsItems(state, onIntent)
        recordingSettingsItems(state, onIntent)
        locationSettingsItems(state, onIntent)
        diagnosticsSettingsItems(state, onIntent)
        appSettingsItems(onIntent)
    }
}

private fun LazyListScope.appearanceSettingsItems(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.settings_appearance_category)) }
    item { ThemeModeSetting(state.preferences.display.themeMode, onIntent) }
    item {
        BooleanSetting(
            title = stringResource(R.string.dynamic_colors),
            description = stringResource(R.string.dynamic_colors_description),
            checked = state.preferences.display.dynamicColors,
        ) { onIntent(SettingsIntent.SetDynamicColors(it)) }
    }
}

private fun LazyListScope.recordingSettingsItems(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.settings_recording_category)) }
    item {
        SamplingSetting(state.preferences.recording.sensorSamplingPeriod) { index ->
            onIntent(SettingsIntent.SetSamplingPeriod(index))
        }
    }
    item { BatteryGuardSetting(state, onIntent) }
    item { BatteryOptimizationSetting(state.isBatteryOptimizationExempt, onIntent) }
    item { CpuWakeLockSetting(state, onIntent) }
    item { ScreenAwakeSetting(state, onIntent) }
}

private fun LazyListScope.locationSettingsItems(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.settings_location_category)) }
    item { GpsSettings(state, onIntent) }
}

private fun LazyListScope.diagnosticsSettingsItems(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.diagnostics_title)) }
    item { DiagnosticsSetting(state, onIntent) }
}

private fun LazyListScope.appSettingsItems(onIntent: (SettingsIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.settings_app_category)) }
    item { AboutSetting(onIntent) }
}
