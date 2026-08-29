package com.tomasrepcik.sensorbox.recording.active

import android.content.res.Resources
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import com.tomasrepcik.sensorbox.design.SensorBoxDangerButton
import com.tomasrepcik.sensorbox.design.SensorBoxPrimaryButton
import com.tomasrepcik.sensorbox.design.SensorBoxRecording
import com.tomasrepcik.sensorbox.design.SensorBoxSettingsDivider
import com.tomasrepcik.sensorbox.recording.RecordingIntent
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState

@Composable
internal fun AnnotationEditor(onIntent: (RecordingIntent) -> Unit) {
    var annotation by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.annotation_description),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = annotation,
            onValueChange = { annotation = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.annotation)) },
            singleLine = true,
        )
        AddAnnotationButton(
            enabled = annotation.isNotBlank(),
            onClick = {
                annotation.trim().takeIf(String::isNotEmpty)?.let {
                    onIntent(RecordingIntent.AddAnnotation(it))
                    annotation = ""
                }
            },
        )
    }
    SensorBoxSettingsDivider()
}

@Composable
private fun AddAnnotationButton(enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        SensorBoxPrimaryButton(
            label = stringResource(R.string.add_annotation),
            onClick = onClick,
            modifier = Modifier.widthIn(min = 152.dp, max = 196.dp),
            enabled = enabled,
        )
    }
}

@Composable
internal fun RecordingHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(64.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecordingIndicator()
        Spacer(Modifier.size(8.dp))
        Text(
            stringResource(R.string.recording),
            color = SensorBoxRecording,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun RecordingIndicator() {
    val transition = rememberInfiniteTransition(label = "recording pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = RECORDING_DIM_ALPHA,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(RECORDING_PULSE_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recording alpha",
    )
    val pulseScale by transition.animateFloat(
        initialValue = RECORDING_MIN_SCALE,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(RECORDING_PULSE_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "recording scale",
    )
    Box(
        Modifier
            .size(12.dp)
            .graphicsLayer {
                alpha = pulseAlpha
                scaleX = pulseScale
                scaleY = pulseScale
            }
            .clip(CircleShape)
            .background(SensorBoxRecording),
    )
}

@Composable
internal fun RecordingTimer(elapsedSeconds: Long, folderName: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.elapsed_time), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(ValueFormats.elapsedSeconds(elapsedSeconds), style = MaterialTheme.typography.displayLarge)
        Text(folderName, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun RecordingSummary(state: RecordingState, session: RecordingSessionState.Running) {
    var expanded by remember { mutableStateOf(false) }
    val sources = recordingSourceNames(state, session, LocalContext.current.resources)
    Column(Modifier.fillMaxWidth()) {
        SourceListToggle(sourceCount = sources.size, expanded = expanded, onClick = { expanded = !expanded })
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            RecordingSourceList(sources)
        }
        SensorBoxSettingsDivider()
    }
}

@Composable
private fun SourceListToggle(sourceCount: Int, expanded: Boolean, onClick: () -> Unit) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "sensor list arrow",
    )
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = stringResource(R.string.sources), style = MaterialTheme.typography.titleMedium)
            Text(
                text = pluralStringResource(R.plurals.source_count, sourceCount, sourceCount),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_expand_more_24),
            contentDescription = stringResource(if (expanded) R.string.hide_sensor_list else R.string.show_sensor_list),
            modifier = Modifier.size(28.dp).rotate(arrowRotation),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecordingSourceList(sources: List<String>) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        sources.forEach { source ->
            Text(
                source,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun recordingSourceNames(
    state: RecordingState,
    session: RecordingSessionState.Running,
    resources: Resources,
): List<String> = buildList {
    session.sensorIds.forEach { sensorId ->
        val sensorName = state.sensors.firstOrNull { sensor -> sensor.type == sensorId }?.name
            ?: resources.getString(R.string.sensor_number, sensorId)
        add(sensorName)
    }
    if (session.includesGps) add(resources.getString(R.string.gps))
}

@Composable
internal fun ActiveRecordingTopBar(onIntent: (RecordingIntent) -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        SensorBoxDangerButton(
            label = stringResource(R.string.stop_and_save),
            onClick = { onIntent(RecordingIntent.StopRecording) },
            modifier = Modifier.widthIn(min = 176.dp, max = 240.dp),
        )
    }
}

private const val RECORDING_PULSE_MILLIS = 850
private const val RECORDING_DIM_ALPHA = 0.28f
private const val RECORDING_MIN_SCALE = 0.78f
