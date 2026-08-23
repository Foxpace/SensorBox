package com.motionapps.sensorbox.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.motionapps.sensorbox.R

@Composable
fun MeasurementSetupScreen(
    state: MainState,
    onIntent: (MainIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { onIntent(MainIntent.ReturnToSensorSelection) },
) {
    Box(modifier.fillMaxSize()) {
        MeasurementSetupContent(state, onIntent, onBack)
        MeasurementSetupActionBar(state, onIntent, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MeasurementSetupContent(state: MainState, onIntent: (MainIntent) -> Unit, onBack: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SensorBoxTopAppBar(stringResource(R.string.measurement_setup), onBack) }
        item { StorageSetupPanel(state.storagePath) { onIntent(MainIntent.ChooseStorage) } }
        item { MeasurementNameSetup(state, onIntent) }
        item { TimingSetup(state, onIntent) }
        item { NotesAndAlarmsSetup(state, onIntent) }
        item { SamplingSetting(state.preferences.sensorSamplingPeriod, onIntent) }
        item { SpecializedSourcesSetup(state, onIntent) }
        item { BatterySetup(state, onIntent) }
        item { WakeLockSetup(state, onIntent) }
        item { KeepScreenAwakeSetup(state, onIntent) }
        if (state.includesGps) {
            item { GpsIntervalSetup(state, onIntent) }
            item { GpsDistanceSetup(state, onIntent) }
        }
        item { MainMessageText(state.message) }
    }
}

@Composable
private fun MeasurementNameSetup(state: MainState, onIntent: (MainIntent) -> Unit) {
    SensorBoxPanel {
        OutlinedTextField(
            value = state.customMeasurementName,
            onValueChange = { onIntent(MainIntent.SetCustomMeasurementName(it)) },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            label = { Text(stringResource(R.string.custom_measurement_name)) },
            supportingText = { Text(stringResource(R.string.custom_measurement_name_description)) },
            singleLine = true,
        )
    }
}

@Composable
private fun TimingSetup(state: MainState, onIntent: (MainIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BooleanSetting(
            title = stringResource(R.string.timed_measurement),
            description = stringResource(R.string.timed_measurement_description),
            checked = state.measurementType == "TIMED",
        ) { onIntent(MainIntent.SetMeasurementType(if (it) "TIMED" else "ENDLESS")) }
        StepSetting(
            stringResource(R.string.start_delay),
            state.startDelaySeconds,
            pluralStringResource(R.plurals.seconds_count, state.startDelaySeconds, state.startDelaySeconds),
            0,
            86_400,
        ) { onIntent(MainIntent.SetStartDelay(it)) }
        if (state.measurementType == "TIMED") {
            StepSetting(
                stringResource(R.string.measurement_duration),
                state.durationSeconds.coerceAtLeast(1),
                pluralStringResource(
                    R.plurals.seconds_count,
                    state.durationSeconds.coerceAtLeast(1),
                    state.durationSeconds.coerceAtLeast(1),
                ),
                1,
                86_400,
            ) { onIntent(MainIntent.SetDuration(it)) }
        }
    }
}

@Composable
private fun NotesAndAlarmsSetup(state: MainState, onIntent: (MainIntent) -> Unit) {
    SensorBoxPanel {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.notes,
                onValueChange = { onIntent(MainIntent.SetNotes(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.measurement_notes)) },
                supportingText = { Text(stringResource(R.string.measurement_notes_description)) },
                minLines = 2,
            )
            OutlinedTextField(
                value = state.alarmOffsets,
                onValueChange = { onIntent(MainIntent.SetAlarmOffsets(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.audible_alarm_offsets)) },
                supportingText = { Text(stringResource(R.string.audible_alarm_offsets_description)) },
                singleLine = true,
            )
        }
    }
}

@Composable
private fun SpecializedSourcesSetup(state: MainState, onIntent: (MainIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BooleanSetting(
            title = stringResource(R.string.activity_recognition),
            description = stringResource(R.string.activity_recognition_description),
            checked = state.activityRecognition,
        ) { onIntent(MainIntent.SetActivityRecognition(it)) }
        if (state.activityRecognition) {
            StepSetting(
                stringResource(R.string.activity_recognition_period),
                state.activityRecognitionPeriodSeconds,
                pluralStringResource(
                    R.plurals.seconds_count,
                    state.activityRecognitionPeriodSeconds,
                    state.activityRecognitionPeriodSeconds,
                ),
                1,
                3_600,
            ) { onIntent(MainIntent.SetActivityRecognitionPeriod(it)) }
        }
        BooleanSetting(
            title = stringResource(R.string.significant_motion),
            description = stringResource(R.string.significant_motion_description),
            checked = state.significantMotion,
        ) { onIntent(MainIntent.SetSignificantMotion(it)) }
    }
}

@Composable
private fun StorageSetupPanel(path: String?, onChoose: () -> Unit) {
    SensorBoxPanel {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    painterResource(R.drawable.ic_baseline_folder),
                    null,
                    Modifier.padding(12.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.recording_folder), style = MaterialTheme.typography.titleMedium)
                Text(
                    path ?: stringResource(R.string.choose_recording_folder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SensorBoxSecondaryButton(
                stringResource(if (path == null) R.string.choose else R.string.change),
                onChoose,
            )
        }
    }
}

@Composable
private fun BatterySetup(state: MainState, onIntent: (MainIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.battery_guard),
        description = stringResource(R.string.battery_guard_setup_description),
        checked = state.preferences.restrictMeasurementOnLowBattery,
    ) { onIntent(MainIntent.SetLowBatteryRestriction(it)) }
}

@Composable
private fun WakeLockSetup(state: MainState, onIntent: (MainIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_cpu_awake),
        description = stringResource(R.string.keep_cpu_awake_setup_description),
        checked = state.preferences.useWakeLock,
    ) { onIntent(MainIntent.SetWakeLock(it)) }
}

@Composable
private fun KeepScreenAwakeSetup(state: MainState, onIntent: (MainIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_screen_awake),
        description = stringResource(R.string.keep_screen_awake_setup_description),
        checked = state.preferences.keepPhoneDisplayOn,
    ) { onIntent(MainIntent.SetKeepScreenAwake(it)) }
}

@Composable
private fun GpsIntervalSetup(state: MainState, onIntent: (MainIntent) -> Unit) {
    StepSetting(
        stringResource(R.string.gps_interval),
        state.preferences.gpsIntervalSeconds,
        pluralStringResource(
            R.plurals.seconds_count,
            state.preferences.gpsIntervalSeconds,
            state.preferences.gpsIntervalSeconds,
        ),
        1,
        3_600,
    ) {
        onIntent(MainIntent.SetGpsInterval(it))
    }
}

@Composable
private fun GpsDistanceSetup(state: MainState, onIntent: (MainIntent) -> Unit) {
    StepSetting(
        stringResource(R.string.gps_minimum_distance),
        state.preferences.gpsMinDistanceMeters,
        pluralStringResource(
            R.plurals.meters_count,
            state.preferences.gpsMinDistanceMeters,
            state.preferences.gpsMinDistanceMeters,
        ),
        0,
        10_000,
    ) {
        onIntent(MainIntent.SetGpsDistance(it))
    }
}

@Composable
private fun MeasurementSetupActionBar(state: MainState, onIntent: (MainIntent) -> Unit, modifier: Modifier = Modifier) {
    val sourceCount = setupSourceCount(state)
    SensorBoxBottomAction(
        title = pluralStringResource(R.plurals.source_count, sourceCount, sourceCount),
        description = stringResource(
            if (state.storagePath == null) R.string.folder_required else R.string.ready_to_record,
        ),
        buttonLabel = stringResource(R.string.start_measurement),
        enabled = state.storagePath != null && sourceCount > 0,
        onClick = { onIntent(MainIntent.StartMeasurement) },
        modifier = modifier,
    )
}

private fun setupSourceCount(state: MainState): Int = state.selectedSensorIds.size + state.selectedWearSensorIds.size +
    (if (state.includesGps) 1 else 0) + (if (state.wearIncludesGps) 1 else 0) +
    (if (state.activityRecognition) 1 else 0) + (if (state.significantMotion) 1 else 0)
