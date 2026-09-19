package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxSecondaryButton
import com.tomasrepcik.sensorbox.design.SensorBoxSettingsDivider
import com.tomasrepcik.sensorbox.settings.BooleanSetting
import com.tomasrepcik.sensorbox.settings.SettingsIntent
import com.tomasrepcik.sensorbox.settings.SettingsState

@Composable
internal fun BatteryGuardSetting(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.battery_guard),
        description = stringResource(R.string.battery_guard_settings_description),
        checked = state.preferences.recording.stopRecordingOnLowBattery,
    ) { onIntent(SettingsIntent.SetStopOnLowBattery(it)) }
}

@Composable
internal fun CpuWakeLockSetting(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_cpu_awake),
        description = stringResource(R.string.keep_cpu_awake_settings_description),
        checked = state.preferences.recording.useWakeLock,
    ) { onIntent(SettingsIntent.SetWakeLock(it)) }
}

@Composable
internal fun ScreenAwakeSetting(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_screen_awake),
        description = stringResource(R.string.keep_screen_awake_settings_description),
        checked = state.preferences.display.keepPhoneDisplayOn,
    ) { onIntent(SettingsIntent.SetKeepScreenAwake(it)) }
}

@Composable
internal fun BatteryOptimizationSetting(isExempt: Boolean, onIntent: (SettingsIntent) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.battery_optimization), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(
                if (isExempt) R.string.battery_optimization_exempt else R.string.battery_optimization_restricted,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!isExempt) {
            SensorBoxSecondaryButton(
                label = stringResource(R.string.exclude_from_battery_saving),
                onClick = { onIntent(SettingsIntent.RequestBatteryOptimizationExemption) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    SensorBoxSettingsDivider()
}
