package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxBackScreen
import com.tomasrepcik.sensorbox.design.SensorBoxSettingsSection
import com.tomasrepcik.sensorbox.recording.RecordingMessageText
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.settings.SamplingSetting

@Composable
internal fun RecordingSetupContent(
    state: RecordingState,
    onIntent: (RecordingSetupIntent) -> Unit,
    onBack: () -> Unit,
) {
    SensorBoxBackScreen(
        title = stringResource(R.string.recording_setup),
        onBack = onBack,
        bottomPadding = 24.dp,
        itemSpacing = 0.dp,
    ) {
        storageSetupItems(state, onIntent)
        measurementDetailsSetupItems(state, onIntent)
        timingSetupItems(state, onIntent)
        sourceSetupItems(state, onIntent)
        recordingOptionSetupItems(state, onIntent)
        if (state.includesGps) locationSetupItems(state, onIntent)
        item { RecordingMessageText(state.message) }
    }
}

private fun LazyListScope.storageSetupItems(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.setup_storage_category)) }
    item {
        RecordingArchivePanel(state.recordingArchivePath) {
            onIntent(RecordingSetupIntent.ChooseRecordingArchive)
        }
    }
}

private fun LazyListScope.measurementDetailsSetupItems(
    state: RecordingState,
    onIntent: (RecordingSetupIntent) -> Unit,
) {
    item { SensorBoxSettingsSection(stringResource(R.string.setup_details_category)) }
    item { MeasurementNameSetup(state, onIntent) }
    item { NotesSetup(state, onIntent) }
    item { AlarmsSetup(state, onIntent) }
}

private fun LazyListScope.timingSetupItems(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.setup_timing_category)) }
    item { RecordingTimingSetup(state, onIntent) }
}

private fun LazyListScope.sourceSetupItems(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.setup_sources_category)) }
    item {
        SamplingSetting(state.preferences.recording.sensorSamplingPeriod) { index ->
            onIntent(RecordingSetupIntent.SetSamplingPeriod(index))
        }
    }
    item { ActivityRecognitionSetup(state, onIntent) }
    item { SignificantMotionSetup(state, onIntent) }
}

private fun LazyListScope.recordingOptionSetupItems(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.settings_recording_category)) }
    item { BatterySetup(state, onIntent) }
    item { WakeLockSetup(state, onIntent) }
    item { KeepScreenAwakeSetup(state, onIntent) }
}

private fun LazyListScope.locationSetupItems(state: RecordingState, onIntent: (RecordingSetupIntent) -> Unit) {
    item { SensorBoxSettingsSection(stringResource(R.string.settings_location_category)) }
    item { GpsIntervalSetup(state, onIntent) }
    item { GpsDistanceSetup(state, onIntent) }
}
