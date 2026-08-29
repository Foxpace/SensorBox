package com.tomasrepcik.sensorbox.presentation.main

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor

@Composable
internal fun SensorSourceItem(
    sensor: SensorDescriptor,
    selected: Boolean,
    onToggle: () -> Unit,
    onInfo: (() -> Unit)?,
) {
    Column {
        SensorRow(sensor, selected, onToggle, onInfo)
        SourceDivider()
    }
}

@Composable
internal fun WearSectionHeader() {
    Text(
        stringResource(R.string.watch_sensors),
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun GpsRow(selected: Boolean, onToggle: () -> Unit, onInfo: (() -> Unit)? = null) {
    SourceRow(
        title = stringResource(R.string.gps),
        icon = R.drawable.ic_source_location,
        selected = selected,
        informationDescription = stringResource(R.string.information_about_gps),
        onToggle = onToggle,
        onInfo = onInfo,
    )
}

@Composable
internal fun RecordHeader(onMeasurements: () -> Unit, onOptions: () -> Unit) {
    SensorBoxTopAppBar(
        title = stringResource(R.string.sources),
        actions = {
            IconButton(onClick = onMeasurements) {
                Icon(
                    painterResource(R.drawable.ic_baseline_folder),
                    contentDescription = stringResource(R.string.measurements),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOptions) {
                Icon(
                    painterResource(R.drawable.ic_baseline_settings_24),
                    contentDescription = stringResource(R.string.options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun SensorRow(sensor: SensorDescriptor, selected: Boolean, onToggle: () -> Unit, onInfo: (() -> Unit)?) {
    SourceRow(
        title = sensor.name,
        icon = sensorIconResource(sensor.type),
        selected = selected,
        informationDescription = stringResource(R.string.information_about_sensor, sensor.name),
        onToggle = onToggle,
        onInfo = onInfo,
    )
}

@Composable
private fun SourceRow(
    title: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    informationDescription: String,
    onToggle: () -> Unit,
    onInfo: (() -> Unit)?,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(18.dp))
        Text(
            title,
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        onInfo?.let { SourceInfoButton(informationDescription, it) }
        Spacer(Modifier.width(4.dp))
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun SourceInfoButton(description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(R.drawable.ic_info),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
internal fun SourceDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
}
