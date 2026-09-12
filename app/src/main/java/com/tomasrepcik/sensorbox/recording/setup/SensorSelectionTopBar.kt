package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxPrimaryButton
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.active.RecordIntent

@Composable
internal fun SensorSelectionTopBar(
    state: RecordingState,
    onIntent: (RecordIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sensorCount = selectedSourceCount(state)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp,
    ) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            SensorBoxPrimaryButton(
                label = stringResource(R.string.continue_action),
                onClick = { onIntent(RecordIntent.OpenRecordingSetup) },
                modifier = Modifier.fillMaxWidth(),
                enabled = sensorCount > 0,
            )
        }
    }
}

private fun selectedSourceCount(state: RecordingState): Int = state.selectedSensorIds.size +
    state.selectedWatchSensorIds.size +
    (if (state.includesGps) 1 else 0) +
    (if (state.watchIncludesGps) 1 else 0) +
    (if (state.activityRecognition) 1 else 0) +
    (if (state.significantMotion) 1 else 0)
