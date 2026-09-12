package com.tomasrepcik.sensorbox.design

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor
import com.tomasrepcik.sensorbox.recording.sources.sensorIconResource

@Composable
fun SensorIdentity(sensor: SensorDescriptor, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(sensorIconResource(sensor.type)),
        contentDescription = sensor.name,
        modifier = modifier.size(54.dp),
    )
}

@Composable
fun SensorSelectionIndicator(selected: Boolean) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier.size(26.dp).border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
    }
}

@Composable
fun SettingSummary(title: String, description: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(14.dp))
        trailing()
    }
}

@Composable
fun SensorBoxSettingsSection(title: String) {
    Text(
        text = title,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun SensorBoxSettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))
}
