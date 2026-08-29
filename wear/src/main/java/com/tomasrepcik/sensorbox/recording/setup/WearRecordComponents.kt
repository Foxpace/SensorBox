package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.WearCheckboxRow
import com.tomasrepcik.sensorbox.design.WearPrimaryEdgeButton
import com.tomasrepcik.sensorbox.home.WearDashboardIntent
import com.tomasrepcik.sensorbox.recording.sources.WatchSensorDescriptor

@Composable
internal fun WearRecordingStartButton(selectedCount: Int, accept: (WearDashboardIntent) -> Unit) {
    WearPrimaryEdgeButton(
        label = pluralStringResource(R.plurals.start_source_count, selectedCount, selectedCount),
        enabled = selectedCount > 0,
        onClick = { accept(WearDashboardIntent.StartRecording) },
    )
}

@Composable
internal fun TransformingLazyColumnItemScope.WearGpsSourceRow(
    selected: Boolean,
    transformation: TransformationSpec,
    onToggle: (Boolean) -> Unit,
) {
    WearCheckboxRow(
        label = stringResource(R.string.gps),
        checked = selected,
        icon = R.drawable.ic_location,
        transformation = transformation,
        onCheckedChange = onToggle,
    )
}

@Composable
internal fun TransformingLazyColumnItemScope.WearSensorSourceRow(
    sensor: WatchSensorDescriptor,
    selected: Boolean,
    transformation: TransformationSpec,
    onToggle: (Boolean) -> Unit,
) {
    WearCheckboxRow(
        label = sensor.name,
        detail = sensor.vendor,
        checked = selected,
        icon = R.drawable.ic_sensor,
        transformation = transformation,
        onCheckedChange = onToggle,
    )
}
