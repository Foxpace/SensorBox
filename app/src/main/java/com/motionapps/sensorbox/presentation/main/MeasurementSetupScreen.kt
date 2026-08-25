package com.motionapps.sensorbox.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.motionapps.sensorbox.R

@Composable
fun MeasurementSetupScreen(
    state: RecordingState,
    onIntent: (RecordingIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { onIntent(RecordingIntent.ReturnToSensorSelection) },
) {
    Box(modifier.fillMaxSize()) {
        MeasurementSetupContent(state, onIntent, onBack)
        MeasurementSetupActionBar(state, onIntent, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MeasurementSetupContent(state: RecordingState, onIntent: (RecordingIntent) -> Unit, onBack: () -> Unit) {
    SensorBoxBackScreen(
        title = stringResource(R.string.measurement_setup),
        onBack = onBack,
        bottomPadding = 104.dp,
        itemSpacing = 0.dp,
    ) {
        item { SensorBoxSettingsSection(stringResource(R.string.setup_storage_category)) }
        item { StorageSetupPanel(state.storagePath) { onIntent(RecordingIntent.ChooseStorage) } }
        item { SensorBoxSettingsSection(stringResource(R.string.setup_details_category)) }
        item { MeasurementNameSetup(state, onIntent) }
        item { NotesAndAlarmsSetup(state, onIntent) }
        item { SensorBoxSettingsSection(stringResource(R.string.setup_timing_category)) }
        item { RecordingTimingSetup(state, onIntent) }
        item { SensorBoxSettingsSection(stringResource(R.string.setup_sources_category)) }
        item {
            SamplingSetting(state.preferences.recording.sensorSamplingPeriod) { index ->
                onIntent(RecordingIntent.SetSamplingPeriod(index))
            }
        }
        item { SpecializedSourcesSetup(state, onIntent) }
        item { SensorBoxSettingsSection(stringResource(R.string.settings_recording_category)) }
        item { BatterySetup(state, onIntent) }
        item { WakeLockSetup(state, onIntent) }
        item { KeepScreenAwakeSetup(state, onIntent) }
        if (state.includesGps) {
            item { SensorBoxSettingsSection(stringResource(R.string.settings_location_category)) }
            item { GpsIntervalSetup(state, onIntent) }
            item { GpsDistanceSetup(state, onIntent) }
        }
        item { RecordingMessageText(state.message) }
    }
}

@Composable
private fun MeasurementNameSetup(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    OutlinedTextField(
        value = state.customMeasurementName,
        onValueChange = { onIntent(RecordingIntent.SetCustomMeasurementName(it)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        label = { Text(stringResource(R.string.custom_measurement_name)) },
        supportingText = { Text(stringResource(R.string.custom_measurement_name_description)) },
        singleLine = true,
    )
    SensorBoxSettingsDivider()
}

@Composable
private fun RecordingTimingSetup(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    Column {
        StepSetting(
            stringResource(R.string.start_delay),
            state.startDelaySeconds,
            pluralStringResource(R.plurals.seconds_count, state.startDelaySeconds, state.startDelaySeconds),
            0,
            86_400,
        ) { onIntent(RecordingIntent.SetStartDelay(it)) }
        StepSetting(
            stringResource(R.string.measurement_duration),
            state.durationSeconds,
            pluralStringResource(R.plurals.seconds_count, state.durationSeconds, state.durationSeconds),
            0,
            86_400,
        ) { onIntent(RecordingIntent.SetDuration(it)) }
    }
}

@Composable
private fun NotesAndAlarmsSetup(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    OutlinedTextField(
        value = state.notes,
        onValueChange = { onIntent(RecordingIntent.SetNotes(it)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        label = { Text(stringResource(R.string.measurement_notes)) },
        supportingText = { Text(stringResource(R.string.measurement_notes_description)) },
        minLines = 2,
    )
    SensorBoxSettingsDivider()
    OutlinedTextField(
        value = state.alarmOffsets,
        onValueChange = { onIntent(RecordingIntent.SetAlarmOffsets(it)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        label = { Text(stringResource(R.string.audible_alarm_offsets)) },
        supportingText = { Text(stringResource(R.string.audible_alarm_offsets_description)) },
        singleLine = true,
    )
    SensorBoxSettingsDivider()
}

@Composable
private fun SpecializedSourcesSetup(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    Column {
        BooleanSetting(
            title = stringResource(R.string.activity_recognition),
            description = stringResource(R.string.activity_recognition_description),
            checked = state.activityRecognition,
        ) { onIntent(RecordingIntent.SetActivityRecognition(it)) }
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
            ) { onIntent(RecordingIntent.SetActivityRecognitionPeriod(it)) }
        }
        BooleanSetting(
            title = stringResource(R.string.significant_motion),
            description = stringResource(R.string.significant_motion_description),
            checked = state.significantMotion,
        ) { onIntent(RecordingIntent.SetSignificantMotion(it)) }
    }
}

@Composable
private fun StorageSetupPanel(path: String?, onChoose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.recording_folder), style = MaterialTheme.typography.titleMedium)
            Text(
                path ?: stringResource(R.string.choose_recording_folder),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(14.dp))
        SensorBoxSecondaryButton(
            stringResource(if (path == null) R.string.choose else R.string.change),
            onChoose,
        )
    }
    SensorBoxSettingsDivider()
}

@Composable
private fun BatterySetup(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.battery_guard),
        description = stringResource(R.string.battery_guard_setup_description),
        checked = state.preferences.recording.restrictMeasurementOnLowBattery,
    ) { onIntent(RecordingIntent.SetLowBatteryRestriction(it)) }
}

@Composable
private fun WakeLockSetup(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_cpu_awake),
        description = stringResource(R.string.keep_cpu_awake_setup_description),
        checked = state.preferences.recording.useWakeLock,
    ) { onIntent(RecordingIntent.SetWakeLock(it)) }
}

@Composable
private fun KeepScreenAwakeSetup(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_screen_awake),
        description = stringResource(R.string.keep_screen_awake_setup_description),
        checked = state.preferences.display.keepPhoneDisplayOn,
    ) { onIntent(RecordingIntent.SetKeepScreenAwake(it)) }
}

@Composable
private fun GpsIntervalSetup(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    StepSetting(
        stringResource(R.string.gps_interval),
        state.preferences.recording.gpsIntervalSeconds,
        pluralStringResource(
            R.plurals.seconds_count,
            state.preferences.recording.gpsIntervalSeconds,
            state.preferences.recording.gpsIntervalSeconds,
        ),
        1,
        3_600,
    ) {
        onIntent(RecordingIntent.SetGpsInterval(it))
    }
}

@Composable
private fun GpsDistanceSetup(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    StepSetting(
        stringResource(R.string.gps_minimum_distance),
        state.preferences.recording.gpsMinDistanceMeters,
        pluralStringResource(
            R.plurals.meters_count,
            state.preferences.recording.gpsMinDistanceMeters,
            state.preferences.recording.gpsMinDistanceMeters,
        ),
        0,
        10_000,
    ) {
        onIntent(RecordingIntent.SetGpsDistance(it))
    }
}

@Composable
private fun MeasurementSetupActionBar(
    state: RecordingState,
    onIntent: (RecordingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourceCount = setupSourceCount(state)
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            SensorBoxPrimaryButton(
                label = stringResource(R.string.start_measurement),
                onClick = { onIntent(RecordingIntent.StartMeasurement) },
                modifier = Modifier.widthIn(min = 176.dp, max = 240.dp),
                enabled = state.storagePath != null && sourceCount > 0,
            )
        }
    }
}

private fun setupSourceCount(state: RecordingState): Int =
    state.selectedSensorIds.size + state.selectedWearSensorIds.size +
        (if (state.includesGps) 1 else 0) + (if (state.wearIncludesGps) 1 else 0) +
        (if (state.activityRecognition) 1 else 0) + (if (state.significantMotion) 1 else 0)
