package com.tomasrepcik.sensorbox.presentation.main

import android.content.Context
import android.os.PowerManager
import android.widget.NumberPicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.preferences.AppThemeMode

@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { onIntent(SettingsIntent.Navigate(MainRoute.RECORD)) },
) {
    val isBatteryOptimizationExempt = rememberBatteryOptimizationExemption()
    LaunchedEffect(Unit) { onIntent(SettingsIntent.ViewDiagnostics) }
    SensorBoxBackScreen(
        title = stringResource(R.string.recording_settings),
        onBack = onBack,
        modifier = modifier,
        itemSpacing = 0.dp,
    ) {
        item { SensorBoxSettingsSection(stringResource(R.string.settings_appearance_category)) }
        item { ThemeModeSetting(state.preferences.display.themeMode, onIntent) }
        item {
            BooleanSetting(
                title = stringResource(R.string.dynamic_colors),
                description = stringResource(R.string.dynamic_colors_description),
                checked = state.preferences.display.dynamicColors,
            ) { onIntent(SettingsIntent.SetDynamicColors(it)) }
        }
        item { SensorBoxSettingsSection(stringResource(R.string.settings_recording_category)) }
        item {
            SamplingSetting(state.preferences.recording.sensorSamplingPeriod) { index ->
                onIntent(SettingsIntent.SetSamplingPeriod(index))
            }
        }
        item { BatteryGuardSetting(state, onIntent) }
        item { BatteryOptimizationSetting(isBatteryOptimizationExempt, onIntent) }
        item { CpuWakeLockSetting(state, onIntent) }
        item { ScreenAwakeSetting(state, onIntent) }
        item { SensorBoxSettingsSection(stringResource(R.string.settings_location_category)) }
        item { GpsSettings(state, onIntent) }
        item { SensorBoxSettingsSection(stringResource(R.string.diagnostics_title)) }
        item { DiagnosticsSetting(state, onIntent) }
        item { SensorBoxSettingsSection(stringResource(R.string.settings_app_category)) }
        item { AboutSetting(onIntent) }
    }
}

@Composable
private fun ThemeModeSetting(selected: AppThemeMode, onIntent: (SettingsIntent) -> Unit) {
    SettingsChoiceSetting(
        title = stringResource(R.string.theme_mode),
        description = stringResource(R.string.theme_mode_description),
        options = listOf(
            AppThemeMode.AUTOMATIC to stringResource(R.string.theme_automatic),
            AppThemeMode.LIGHT to stringResource(R.string.theme_light),
            AppThemeMode.DARK to stringResource(R.string.theme_dark),
        ),
        selected = selected,
    ) { onIntent(SettingsIntent.SetThemeMode(it)) }
}

@Composable
private fun DiagnosticsSetting(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    if (!state.diagnosticsLoaded) {
        Text(
            stringResource(R.string.diagnostics_loading),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else if (state.diagnosticsText.isNullOrBlank()) {
        Text(
            stringResource(R.string.diagnostics_empty),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        SettingsControlRow(
            title = stringResource(R.string.diagnostics_view),
            description = stringResource(R.string.diagnostics_view_summary),
        ) { onIntent(SettingsIntent.Navigate(MainRoute.DIAGNOSTICS)) }
        SettingsControlRow(
            title = stringResource(R.string.diagnostics_share),
            description = stringResource(R.string.diagnostics_share_summary),
        ) { onIntent(SettingsIntent.ShareDiagnosticsFile) }
        SettingsControlRow(
            title = stringResource(R.string.diagnostics_clear),
            description = stringResource(R.string.diagnostics_clear_summary),
            showDivider = false,
        ) { confirmClear = true }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.diagnostics_clear)) },
            text = { Text(stringResource(R.string.diagnostics_clear_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onIntent(SettingsIntent.ClearDiagnostics)
                    },
                ) {
                    Text(stringResource(R.string.diagnostics_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun GpsSettings(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    Column {
        NumberPickerSetting(
            stringResource(R.string.gps_interval),
            state.preferences.recording.gpsIntervalSeconds,
            pluralStringResource(
                R.plurals.seconds_count,
                state.preferences.recording.gpsIntervalSeconds,
                state.preferences.recording.gpsIntervalSeconds,
            ),
            1,
            3_600,
        ) { onIntent(SettingsIntent.SetGpsInterval(it)) }
        NumberPickerSetting(
            stringResource(R.string.gps_minimum_distance),
            state.preferences.recording.gpsMinDistanceMeters,
            pluralStringResource(
                R.plurals.meters_count,
                state.preferences.recording.gpsMinDistanceMeters,
                state.preferences.recording.gpsMinDistanceMeters,
            ),
            0,
            10_000,
        ) { onIntent(SettingsIntent.SetGpsDistance(it)) }
    }
}

@Composable
private fun NumberPickerSetting(
    title: String,
    value: Int,
    valueLabel: String,
    minimum: Int,
    maximum: Int,
    onValue: (Int) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(valueLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = { showPicker = true }) {
            Text(value.toString())
        }
    }
    SensorBoxSettingsDivider()

    if (showPicker) {
        var selectedValue by remember(value, minimum, maximum) {
            mutableIntStateOf(value.coerceIn(minimum, maximum))
        }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(title) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AndroidView(
                        factory = { context ->
                            NumberPicker(context).apply {
                                minValue = minimum
                                maxValue = maximum
                                wrapSelectorWheel = false
                                this.value = selectedValue
                                setOnValueChangedListener { _, _, newValue -> selectedValue = newValue }
                            }
                        },
                        update = { picker ->
                            if (picker.value != selectedValue) picker.value = selectedValue
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPicker = false
                        onValue(selectedValue)
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun BatteryGuardSetting(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.battery_guard),
        description = stringResource(R.string.battery_guard_settings_description),
        checked = state.preferences.recording.stopRecordingOnLowBattery,
    ) { onIntent(SettingsIntent.SetStopOnLowBattery(it)) }
}

@Composable
private fun CpuWakeLockSetting(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_cpu_awake),
        description = stringResource(R.string.keep_cpu_awake_settings_description),
        checked = state.preferences.recording.useWakeLock,
    ) { onIntent(SettingsIntent.SetWakeLock(it)) }
}

@Composable
private fun ScreenAwakeSetting(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_screen_awake),
        description = stringResource(R.string.keep_screen_awake_settings_description),
        checked = state.preferences.display.keepPhoneDisplayOn,
    ) { onIntent(SettingsIntent.SetKeepScreenAwake(it)) }
}

@Composable
private fun BatteryOptimizationSetting(isExempt: Boolean, onIntent: (SettingsIntent) -> Unit) {
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

@Composable
private fun AboutSetting(onIntent: (SettingsIntent) -> Unit) {
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    SettingsControlRow(
        title = stringResource(R.string.menu_about),
        description = stringResource(R.string.about_summary),
        showDivider = false,
        showChevron = true,
    ) { showAboutDialog = true }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onPrivacy = {
                showAboutDialog = false
                onIntent(SettingsIntent.Navigate(MainRoute.PRIVACY))
            },
            onLicenses = {
                showAboutDialog = false
                onIntent(SettingsIntent.Navigate(MainRoute.LICENSES))
            },
        )
    }
}

@Composable
private fun rememberBatteryOptimizationExemption(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isExempt by remember(context) { mutableStateOf(context.isBatteryOptimizationExempt()) }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isExempt = context.isBatteryOptimizationExempt()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return isExempt
}

private fun Context.isBatteryOptimizationExempt(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

@Composable
fun SamplingSetting(selected: Int, onSamplingPeriod: (Int) -> Unit) {
    SettingsChoiceSetting(
        title = stringResource(R.string.sensor_sampling),
        description = stringResource(R.string.sensor_sampling_description),
        options = listOf(
            0 to stringResource(R.string.sampling_fastest),
            1 to stringResource(R.string.sampling_game),
            2 to stringResource(R.string.sampling_ui),
            3 to stringResource(R.string.sampling_normal),
        ),
        selected = selected,
        onSelected = onSamplingPeriod,
    )
}

@Composable
private fun <T> SettingsChoiceSetting(
    title: String,
    description: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            options.forEach { (value, label) ->
                SettingsChoiceChip(label, selected == value) {
                    onSelected(value)
                }
            }
        }
    }
    SensorBoxSettingsDivider()
}

@Composable
fun BooleanSetting(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(14.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
    SensorBoxSettingsDivider()
}

@Composable
private fun SettingsControlRow(
    title: String,
    description: String,
    showDivider: Boolean = true,
    showChevron: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showChevron) {
            Spacer(Modifier.width(14.dp))
            Icon(
                painter = painterResource(R.drawable.ic_expand_more_24),
                contentDescription = null,
                modifier = Modifier.size(24.dp).rotate(-90f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showDivider) SensorBoxSettingsDivider()
}

@Composable
private fun SettingsChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
fun StepSetting(title: String, value: Int, valueLabel: String, minimum: Int, maximum: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(valueLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StepButton(stringResource(R.string.decrement)) { onValue((value - 1).coerceAtLeast(minimum)) }
        Spacer(Modifier.width(8.dp))
        StepButton(stringResource(R.string.increment)) { onValue((value + 1).coerceAtMost(maximum)) }
    }
    SensorBoxSettingsDivider()
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
