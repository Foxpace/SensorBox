package com.motionapps.sensorbox.presentation.main

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.motionapps.sensorbox.R
import com.motionapps.sensorbox.ui.theme.SensorBoxRecording
import com.motionapps.sensorservices.session.MeasurementSessionState

@Composable
fun ActiveMeasurementScreen(state: RecordingState, onIntent: (RecordingIntent) -> Unit, modifier: Modifier = Modifier) {
    val session = state.session as? MeasurementSessionState.Running ?: return
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RecordingHeader()
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            MeasurementTimer(state.elapsedSeconds, session.folderName)
            Spacer(Modifier.height(36.dp))
            MeasurementSummary(state, session)
            Spacer(Modifier.height(24.dp))
            AnnotationEditor(onIntent)
            Spacer(Modifier.height(24.dp))
        }
        SensorBoxDangerButton(
            label = stringResource(R.string.stop_and_save),
            onClick = { onIntent(RecordingIntent.StopMeasurement) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AnnotationEditor(onIntent: (RecordingIntent) -> Unit) {
    var annotation by remember { mutableStateOf("") }
    SensorBoxPanel {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = annotation,
                onValueChange = { annotation = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.annotation)) },
                supportingText = { Text(stringResource(R.string.annotation_description)) },
                singleLine = true,
            )
            SensorBoxSecondaryButton(
                label = stringResource(R.string.add_annotation),
                onClick = {
                    annotation.trim().takeIf(String::isNotEmpty)?.let {
                        onIntent(RecordingIntent.AddAnnotation(it))
                        annotation = ""
                    }
                },
            )
        }
    }
}

@Composable
private fun RecordingHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
private fun MeasurementTimer(elapsedSeconds: Long, folderName: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.elapsed_time), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text(formatElapsed(elapsedSeconds), style = MaterialTheme.typography.displayLarge)
        Text(folderName, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MeasurementSummary(state: RecordingState, session: MeasurementSessionState.Running) {
    var expanded by remember { mutableStateOf(false) }
    val sources = recordingSourceNames(state, session, LocalContext.current.resources)
    SensorBoxPanel {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryValue(sources.size.toString(), stringResource(R.string.sources))
                SummaryValue(
                    stringResource(if (session.includesGps) R.string.on else R.string.off),
                    stringResource(R.string.gps),
                )
            }
            SensorListToggle(expanded = expanded, onClick = { expanded = !expanded })
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                RecordingSourceList(sources)
            }
        }
    }
}

@Composable
private fun SensorListToggle(expanded: Boolean, onClick: () -> Unit) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "sensor list arrow",
    )
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.sensor_list),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        sources.forEach { source ->
            Text(source, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun recordingSourceNames(
    state: RecordingState,
    session: MeasurementSessionState.Running,
    resources: Resources,
): List<String> = buildList {
    session.sensorIds.forEach { sensorId ->
        val sensorName = state.sensors.firstOrNull { sensor ->
            sensor.type == sensorId
        }?.name ?: resources.getString(R.string.sensor_number, sensorId)
        add(sensorName)
    }
    if (session.includesGps) add(resources.getString(R.string.gps))
}

@Composable
private fun SummaryValue(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatElapsed(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = seconds % 3_600 / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, remainingSeconds)
}

private const val RECORDING_PULSE_MILLIS = 850
private const val RECORDING_DIM_ALPHA = 0.28f
private const val RECORDING_MIN_SCALE = 0.78f
