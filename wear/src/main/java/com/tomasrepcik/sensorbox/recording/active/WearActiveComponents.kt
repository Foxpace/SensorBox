package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.home.WearDashboardState

@Composable
internal fun WearRecordingStatus(state: WearDashboardState) {
    val session = state.activeSession ?: return
    val sourceCount = session.sensorIds.size + if (session.includesGps) 1 else 0
    Text(
        text = stringResource(R.string.recording),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = pluralStringResource(R.plurals.source_count, sourceCount, sourceCount),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = stringResource(if (session.includesGps) R.string.recording_gps_on else R.string.recording_gps_off),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = if (session.durationMillis > 0) {
            stringResource(R.string.recording_duration_seconds, session.durationMillis / 1_000)
        } else {
            stringResource(R.string.recording_until_stopped)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(samplingLabel(session.sensorSamplingPeriod)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    state.sensors.filter { it.type in session.sensorIds }.forEach { sensor ->
        Text(
            sensor.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

private fun samplingLabel(period: Int): Int = when (period) {
    1 -> R.string.sampling_game
    2 -> R.string.sampling_ui
    3 -> R.string.sampling_normal
    else -> R.string.sampling_fastest
}

@Composable
internal fun WearStopRecordingButton(isStopping: Boolean, modifier: Modifier = Modifier, onStop: () -> Unit) {
    val description = stringResource(R.string.stop_and_save)
    Button(
        onClick = onStop,
        enabled = !isStopping,
        modifier = modifier.height(52.dp).semantics { contentDescription = description },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFB3261E),
            contentColor = Color.White,
        ),
    ) {
        Text(
            stringResource(if (isStopping) R.string.recording_stopping else R.string.stop_recording),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
