package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R

@Composable
internal fun GpsPreview(state: RecordingState, onIntent: (RecordingIntent) -> Unit) {
    DisposableEffect(Unit) {
        onIntent(RecordingIntent.StartGpsPreview)
        onDispose { onIntent(RecordingIntent.StopPreview) }
    }
    val details = state.gpsPreview
    val unavailableValue = stringResource(if (details.hasPermission) R.string.waiting else R.string.unavailable)
    GpsPreviewPanel(details, unavailableValue) { onIntent(RecordingIntent.RequestLocationPreviewPermission) }
}

@Composable
private fun GpsPreviewPanel(
    details: com.tomasrepcik.sensorbox.domain.preview.GpsPreviewData,
    unavailableValue: String,
    onRequestPermission: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        GpsPreviewValues(details, unavailableValue)
        if (!details.hasPermission) GpsPreviewPermissionRequest(onRequestPermission)
    }
}

@Composable
private fun GpsPreviewValues(
    details: com.tomasrepcik.sensorbox.domain.preview.GpsPreviewData,
    unavailableValue: String,
) {
    PreviewValueRow(
        stringResource(R.string.location_permission),
        stringResource(if (details.hasPermission) R.string.granted else R.string.required),
    )
    PreviewValueRow(
        stringResource(R.string.detail_latitude),
        details.location?.latitude?.toString() ?: unavailableValue,
    )
    PreviewValueRow(
        stringResource(R.string.detail_longitude),
        details.location?.longitude?.toString() ?: unavailableValue,
    )
    PreviewValueRow(
        stringResource(R.string.detail_altitude),
        details.location?.altitude?.let(::formatDecimal) ?: unavailableValue,
    )
    PreviewValueRow(
        stringResource(R.string.detail_bearing),
        details.location?.bearing?.let(::formatDecimal) ?: unavailableValue,
        showDivider = false,
    )
}

@Composable
private fun GpsPreviewPermissionRequest(onRequestPermission: () -> Unit) {
    Text(
        stringResource(R.string.location_permission_explanation),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
    )
    SensorBoxSecondaryButton(
        label = stringResource(R.string.grant_location_permission),
        onClick = onRequestPermission,
        modifier = Modifier.fillMaxWidth(),
    )
}
