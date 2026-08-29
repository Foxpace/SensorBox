package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R

@Composable
fun SamplingSetting(selected: Int, onSamplingPeriod: (Int) -> Unit) {
    SettingsChoiceSetting(
        title = stringResource(R.string.sensor_sampling),
        description = stringResource(R.string.sensor_sampling_description),
        options = listOf(
            0 to stringResource(R.string.sampling_fastest),
            1 to stringResource(R.string.sampling_game),
            2 to stringResource(R.string.sampling_ui),
            3 to stringResource(R.string.sampling_normal),
        ),
        selected = selected,
        onSelected = onSamplingPeriod,
    )
}

@Composable
internal fun <T> SettingsChoiceSetting(
    title: String,
    description: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsChoiceHeader(title, description)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            options.forEach { (value, label) ->
                SettingsChoiceChip(label, selected == value) { onSelected(value) }
            }
        }
    }
    SensorBoxSettingsDivider()
}

@Composable
internal fun SettingsControlRow(
    title: String,
    description: String,
    showDivider: Boolean = true,
    showChevron: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingText(title, description, Modifier.weight(1f))
        if (showChevron) {
            Spacer(Modifier.width(14.dp))
            Icon(
                painter = painterResource(R.drawable.ic_expand_more_24),
                contentDescription = null,
                modifier = Modifier.size(24.dp).rotate(-90f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (showDivider) SensorBoxSettingsDivider()
}

@Composable
private fun SettingsChoiceHeader(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    )
}

@Composable
fun BooleanSetting(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingText(title, description, Modifier.weight(1f))
        Spacer(Modifier.width(14.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
    SensorBoxSettingsDivider()
}

@Composable
fun StepSetting(title: String, value: Int, valueLabel: String, minimum: Int, maximum: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        SettingText(title, valueLabel, Modifier.weight(1f))
        StepButton(stringResource(R.string.decrement)) { onValue((value - 1).coerceAtLeast(minimum)) }
        Spacer(Modifier.width(8.dp))
        StepButton(stringResource(R.string.increment)) { onValue((value + 1).coerceAtMost(maximum)) }
    }
    SensorBoxSettingsDivider()
}

@Composable
private fun SettingText(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
