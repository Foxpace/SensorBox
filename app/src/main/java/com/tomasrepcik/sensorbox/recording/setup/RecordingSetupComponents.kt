package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxSecondaryButton
import com.tomasrepcik.sensorbox.design.SensorBoxSettingsDivider
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.settings.BooleanSetting
import com.tomasrepcik.sensorbox.settings.StepSetting

@Composable
internal fun MeasurementNameSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    OutlinedTextField(
        value = state.customMeasurementName,
        onValueChange = { onIntent(RecordingSetupIntent.SetCustomMeasurementName(it)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        label = { Text(stringResource(R.string.custom_measurement_name)) },
        supportingText = { Text(stringResource(R.string.custom_measurement_name_description)) },
        singleLine = true,
    )
    SensorBoxSettingsDivider()
}

@Composable
internal fun RecordingTimingSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    Column {
        StepSetting(
            stringResource(R.string.start_delay),
            state.startDelaySeconds,
            pluralStringResource(R.plurals.seconds_count, state.startDelaySeconds, state.startDelaySeconds),
            0,
            86_400,
        ) { onIntent(RecordingSetupIntent.SetStartDelay(it)) }
        StepSetting(
            stringResource(R.string.recording_duration),
            state.durationSeconds,
            pluralStringResource(R.plurals.seconds_count, state.durationSeconds, state.durationSeconds),
            0,
            86_400,
        ) { onIntent(RecordingSetupIntent.SetDuration(it)) }
    }
}

@Composable
internal fun NotesSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    OutlinedTextField(
        value = state.notes,
        onValueChange = { onIntent(RecordingSetupIntent.SetNotes(it)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        label = { Text(stringResource(R.string.recording_notes)) },
        supportingText = { Text(stringResource(R.string.recording_notes_description)) },
        minLines = 2,
    )
    SensorBoxSettingsDivider()
}

@Composable
internal fun AlarmsSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    OutlinedTextField(
        value = state.alarmOffsets,
        onValueChange = { onIntent(RecordingSetupIntent.SetAlarmOffsets(it)) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        label = { Text(stringResource(R.string.audible_alarm_offsets)) },
        supportingText = { Text(stringResource(R.string.audible_alarm_offsets_description)) },
        singleLine = true,
    )
    SensorBoxSettingsDivider()
}

@Composable
internal fun ActivityRecognitionSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.activity_recognition),
        description = stringResource(R.string.activity_recognition_description),
        checked = state.activityRecognition,
    ) { onIntent(RecordingSetupIntent.SetActivityRecognition(it)) }
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
        ) { onIntent(RecordingSetupIntent.SetActivityRecognitionPeriod(it)) }
    }
}

@Composable
internal fun SignificantMotionSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.significant_motion),
        description = stringResource(R.string.significant_motion_description),
        checked = state.significantMotion,
    ) { onIntent(RecordingSetupIntent.SetSignificantMotionRecording(it)) }
}

@Composable
internal fun RecordingArchivePanel(path: String?, onChoose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(R.string.recording_archive), style = MaterialTheme.typography.titleMedium)
            Text(
                path ?: stringResource(R.string.choose_recording_archive),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(14.dp))
        SensorBoxSecondaryButton(stringResource(if (path == null) R.string.choose else R.string.change), onChoose)
    }
    SensorBoxSettingsDivider()
}

@Composable
internal fun BatterySetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.battery_guard),
        description = stringResource(R.string.battery_guard_setup_description),
        checked = state.preferences.recording.stopRecordingOnLowBattery,
    ) { onIntent(RecordingSetupIntent.SetStopOnLowBattery(it)) }
}

@Composable
internal fun WakeLockSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_cpu_awake),
        description = stringResource(R.string.keep_cpu_awake_setup_description),
        checked = state.preferences.recording.useWakeLock,
    ) { onIntent(RecordingSetupIntent.SetWakeLock(it)) }
}

@Composable
internal fun KeepScreenAwakeSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    BooleanSetting(
        title = stringResource(R.string.keep_screen_awake),
        description = stringResource(R.string.keep_screen_awake_setup_description),
        checked = state.preferences.display.keepPhoneDisplayOn,
    ) { onIntent(RecordingSetupIntent.SetKeepScreenAwake(it)) }
}

@Composable
internal fun GpsIntervalSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    val seconds = state.preferences.recording.gpsIntervalSeconds
    StepSetting(
        stringResource(R.string.gps_interval),
        seconds,
        pluralStringResource(R.plurals.seconds_count, seconds, seconds),
        1,
        3_600,
    ) { onIntent(RecordingSetupIntent.SetGpsInterval(it)) }
}

@Composable
internal fun GpsDistanceSetup(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    val meters = state.preferences.recording.gpsMinDistanceMeters
    StepSetting(
        stringResource(R.string.gps_minimum_distance),
        meters,
        pluralStringResource(R.plurals.meters_count, meters, meters),
        0,
        10_000,
    ) { onIntent(RecordingSetupIntent.SetGpsDistance(it)) }
}
