package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.NumberPickerSetting
import com.tomasrepcik.sensorbox.settings.SettingsIntent
import com.tomasrepcik.sensorbox.settings.SettingsState

@Composable
internal fun GpsSettings(state: SettingsState, onIntent: (SettingsIntent) -> Unit) {
    val intervalSeconds = state.preferences.recording.gpsIntervalSeconds
    val distanceMeters = state.preferences.recording.gpsMinDistanceMeters
    Column {
        NumberPickerSetting(
            stringResource(R.string.gps_interval),
            intervalSeconds,
            pluralStringResource(R.plurals.seconds_count, intervalSeconds, intervalSeconds),
            1,
            3_600,
        ) { onIntent(SettingsIntent.SetGpsInterval(it)) }
        NumberPickerSetting(
            stringResource(R.string.gps_minimum_distance),
            distanceMeters,
            pluralStringResource(R.plurals.meters_count, distanceMeters, distanceMeters),
            0,
            10_000,
        ) { onIntent(SettingsIntent.SetGpsDistance(it)) }
    }
}
