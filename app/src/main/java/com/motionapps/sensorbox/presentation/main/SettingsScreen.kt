package com.motionapps.sensorbox.presentation.main

import android.content.Context
import android.os.PowerManager
import android.widget.NumberPicker
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.motionapps.sensorbox.R

@Composable
fun SettingsScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { onIntent(MainIntent.Navigate(MainRoute.RECORD)) },
) {
    val isBatteryOptimizationExempt = rememberBatteryOptimizationExemption()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SensorBoxTopAppBar(stringResource(R.string.measurement_settings), onBack)
        }
        item { SamplingSetting(state.preferences.sensorSamplingPeriod, onIntent) }
        item { BatteryGuardSetting(state, onIntent) }
        item { BatteryOptimizationSetting(isBatteryOptimizationExempt, onIntent) }
        item { CpuWakeLockSetting(state, onIntent) }
        item { ScreenAwakeSetting(state, onIntent) }
        item { GpsSettings(state, onIntent) }
        item { DiagnosticsSetting(onIntent) }
        item { AboutSetting(onIntent) }
    }
}

@Composable
private fun DiagnosticsSetting(onIntent: (MainIntent) -> Unit) {
    SensorBoxPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.diagnostics_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.diagnostics_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SensorBoxSecondaryButton(
                label = stringResource(R.string.diagnostics_share_text),
                onClick = { onIntent(MainIntent.ShareDiagnosticsText) },
                modifier = Modifier.fillMaxWidth(),
            )
            SensorBoxSecondaryButton(
                label = stringResource(R.string.diagnostics_share_file),
                onClick = { onIntent(MainIntent.ShareDiagnosticsFile) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GpsSettings(state: MainState, onIntent: (MainIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NumberPickerSetting(
            stringResource(R.string.gps_interval),
            state.preferences.gpsIntervalSeconds,
            pluralStringResource(
                R.plurals.seconds_count,
                state.preferences.gpsIntervalSeconds,
                state.preferences.gpsIntervalSeconds,
            ),
            1,
            3_600,
        ) { onIntent(MainIntent.SetGpsInterval(it)) }
        NumberPickerSetting(
            stringResource(R.string.gps_minimum_distance),
            state.preferences.gpsMinDistanceMeters,
            pluralStringResource(
                R.plurals.meters_count,
                state.preferences.gpsMinDistanceMeters,
                state.preferences.gpsMinDistanceMeters,
            ),
            0,
            10_000,
        ) { onIntent(MainIntent.SetGpsDistance(it)) }
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

    SensorBoxPanel {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(valueLabel, color = MaterialTheme.colorScheme.primary)
            }
            OutlinedButton(onClick = { showPicker = true }) {
                Text(value.toString())
            }
        }
    }

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
private fun BatteryGuardSetting(state: MainState, onIntent: (MainIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.battery_guard),
        description = stringResource(R.string.battery_guard_settings_description),
        checked = state.preferences.restrictMeasurementOnLowBattery,
    ) { onIntent(MainIntent.SetLowBatteryRestriction(it)) }
}

@Composable
private fun CpuWakeLockSetting(state: MainState, onIntent: (MainIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_cpu_awake),
        description = stringResource(R.string.keep_cpu_awake_settings_description),
        checked = state.preferences.useWakeLock,
    ) { onIntent(MainIntent.SetWakeLock(it)) }
}

@Composable
private fun ScreenAwakeSetting(state: MainState, onIntent: (MainIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_screen_awake),
        description = stringResource(R.string.keep_screen_awake_settings_description),
        checked = state.preferences.keepPhoneDisplayOn,
    ) { onIntent(MainIntent.SetKeepScreenAwake(it)) }
}

@Composable
private fun BatteryOptimizationSetting(isExempt: Boolean, onIntent: (MainIntent) -> Unit) {
    SensorBoxPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.battery_optimization), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    if (isExempt) R.string.battery_optimization_exempt else R.string.battery_optimization_restricted,
                ),
                color = if (isExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!isExempt) {
                SensorBoxSecondaryButton(
                    label = stringResource(R.string.exclude_from_battery_saving),
                    onClick = { onIntent(MainIntent.RequestBatteryOptimizationExemption) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AboutSetting(onIntent: (MainIntent) -> Unit) {
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }

    SensorBoxPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.about_summary),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SensorBoxSecondaryButton(
                label = stringResource(R.string.menu_about),
                onClick = { showAboutDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onPrivacy = {
                showAboutDialog = false
                onIntent(MainIntent.Navigate(MainRoute.PRIVACY))
            },
            onLicenses = {
                showAboutDialog = false
                onIntent(MainIntent.Navigate(MainRoute.LICENSES))
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
fun SamplingSetting(selected: Int, onIntent: (MainIntent) -> Unit) {
    SensorBoxPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.sensor_sampling), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.sensor_sampling_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(
                    R.string.sampling_fastest,
                    R.string.sampling_game,
                    R.string.sampling_ui,
                    R.string.sampling_normal,
                ).forEachIndexed { index, label ->
                    SamplingChip(stringResource(label), selected == index) {
                        onIntent(MainIntent.SetSamplingPeriod(index))
                    }
                }
            }
        }
    }
}

@Composable
private fun SamplingChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
fun BooleanSetting(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    SensorBoxPanel {
        SettingSummary(title, description) {
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
fun StepSetting(title: String, value: Int, valueLabel: String, minimum: Int, maximum: Int, onValue: (Int) -> Unit) {
    SensorBoxPanel {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(valueLabel, color = MaterialTheme.colorScheme.primary)
            }
            StepButton(stringResource(R.string.decrement)) { onValue((value - 1).coerceAtLeast(minimum)) }
            Spacer(Modifier.width(8.dp))
            StepButton(stringResource(R.string.increment)) { onValue((value + 1).coerceAtMost(maximum)) }
        }
    }
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
