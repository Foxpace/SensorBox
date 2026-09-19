package com.tomasrepcik.sensorbox.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.WearListScreen
import com.tomasrepcik.sensorbox.design.WearPageTitle
import com.tomasrepcik.sensorbox.design.WearRadioRow
import com.tomasrepcik.sensorbox.design.WearSectionTitle
import com.tomasrepcik.sensorbox.design.WearSwitchRow
import com.tomasrepcik.sensorbox.home.WearBackButton
import com.tomasrepcik.sensorbox.home.WearDashboardIntent
import com.tomasrepcik.sensorbox.home.WearDashboardState
import com.tomasrepcik.sensorbox.home.wearMessageText

@Composable
internal fun WearSettingsScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    WearListScreen { transformation ->
        item { WearPageTitle(stringResource(R.string.activity_settings), transformation) }
        samplingPreferenceItems(state, transformation, accept)
        recordingPreferenceItems(state, transformation, accept)
        state.message?.let { message ->
            item { WearSectionTitle(wearMessageText(message), transformation) }
        }
        item { WearBackButton(transformation, accept) }
    }
}

private fun TransformingLazyColumnScope.samplingPreferenceItems(
    state: WearDashboardState,
    transformation: TransformationSpec,
    accept: (WearDashboardIntent) -> Unit,
) {
    item { WearSectionTitle(stringResource(R.string.sensor_sampling), transformation) }
    listOf(
        R.string.sampling_fastest,
        R.string.sampling_game,
        R.string.sampling_ui,
        R.string.sampling_normal,
    ).forEachIndexed { index, label ->
        item {
            WearRadioRow(
                label = stringResource(label),
                selected = state.preferences.recording.sensorSamplingPeriod == index,
                transformation = transformation,
            ) { accept(WearDashboardIntent.SetSamplingPeriod(index)) }
        }
    }
}

private fun TransformingLazyColumnScope.recordingPreferenceItems(
    state: WearDashboardState,
    transformation: TransformationSpec,
    accept: (WearDashboardIntent) -> Unit,
) {
    item { WearSectionTitle(stringResource(R.string.recording_options), transformation) }
    item {
        WearSwitchRow(
            label = stringResource(R.string.stop_on_low_battery),
            checked = state.preferences.recording.stopRecordingOnLowBattery,
            transformation = transformation,
            onCheckedChange = { accept(WearDashboardIntent.ToggleBatteryRestriction) },
        )
    }
    item {
        WearSwitchRow(
            label = stringResource(R.string.wake_lock),
            checked = state.preferences.recording.useWakeLock,
            transformation = transformation,
            onCheckedChange = { accept(WearDashboardIntent.ToggleWakeLock) },
        )
    }
    item {
        WearSwitchRow(
            label = stringResource(R.string.keep_display_on),
            checked = state.preferences.display.keepWearDisplayOn,
            transformation = transformation,
            onCheckedChange = { accept(WearDashboardIntent.ToggleDisplay) },
        )
    }
}
