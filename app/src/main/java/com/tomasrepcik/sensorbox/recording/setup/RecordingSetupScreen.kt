package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tomasrepcik.sensorbox.recording.RecordingState

@Composable
fun RecordingSetupScreen(
    state: RecordingState,
    onIntent: (RecordingSetupIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { RecordingSetupContent(state, onIntent, onBack) }
        RecordingSetupTopBar(state, onIntent)
    }
}
