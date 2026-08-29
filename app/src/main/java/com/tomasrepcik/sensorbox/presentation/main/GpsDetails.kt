package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.domain.preview.GpsPreviewData

@Composable
internal fun GpsDetails(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    DisposableEffect(Unit) {
        onIntent(RecordingIntent.StartGpsPreview)
        onDispose { onIntent(RecordingIntent.StopPreview) }
    }
    val details = state.gpsPreview
    val unavailableValue = stringResource(if (details.hasPermission) R.string.waiting else R.string.unavailable)
    Column(Modifier.fillMaxWidth()) {
        GpsPermissionStatus(details.hasPermission)
        GpsPositionRows(details, unavailableValue)
        GpsRecordingRows(state)
        if (!details.hasPermission) {
            GpsPermissionButton { onIntent(RecordingIntent.RequestLocationPreviewPermission) }
        }
    }
}

@Composable
private fun GpsPositionRows(details: GpsPreviewData, unavailableValue: String) {
    ParameterRow(stringResource(R.string.detail_latitude), details.location?.latitude?.toString() ?: unavailableValue)
    ParameterRow(stringResource(R.string.detail_longitude), details.location?.longitude?.toString() ?: unavailableValue)
    ParameterRow(stringResource(R.string.detail_altitude), localizedValue(details.location?.altitude, unavailableValue))
    ParameterRow(stringResource(R.string.detail_accuracy), localizedValue(details.location?.accuracy, unavailableValue))
    ParameterRow(stringResource(R.string.detail_speed), localizedValue(details.location?.speed, unavailableValue))
    ParameterRow(stringResource(R.string.detail_bearing), localizedValue(details.location?.bearing, unavailableValue))
    ParameterRow(stringResource(R.string.detail_provider), details.location?.provider ?: unavailableValue)
    ParameterRow(stringResource(R.string.detail_available), locationAvailabilityLabel(details.isAvailable))
}

@Composable
private fun GpsRecordingRows(state: RecordingState) {
    val seconds = state.preferences.recording.gpsIntervalSeconds
    val meters = state.preferences.recording.gpsMinDistanceMeters
    ParameterRow(
        stringResource(R.string.detail_update_interval),
        pluralStringResource(R.plurals.seconds_count, seconds, seconds),
    )
    ParameterRow(
        stringResource(R.string.detail_minimum_distance),
        pluralStringResource(R.plurals.meters_count, meters, meters),
        showDivider = false,
    )
}

@Composable
private fun GpsPermissionStatus(hasPermission: Boolean) {
    ParameterRow(
        stringResource(R.string.location_permission),
        stringResource(if (hasPermission) R.string.granted else R.string.required),
    )
}

@Composable
private fun GpsPermissionButton(onRequest: () -> Unit) {
    Text(
        stringResource(R.string.location_permission_explanation),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
    )
    SensorBoxSecondaryButton(
        label = stringResource(R.string.grant_location_permission),
        onClick = onRequest,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun localizedValue(value: Number?, unavailableValue: String): String =
    value?.let(::formatDecimal) ?: unavailableValue

@Composable
private fun locationAvailabilityLabel(isAvailable: Boolean?): String = when (isAvailable) {
    true -> stringResource(R.string.yes)
    false -> stringResource(R.string.no)
    null -> stringResource(R.string.unknown)
}
