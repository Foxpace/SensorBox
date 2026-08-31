package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.setup.SensorSelectionTopBar

@Composable
fun RecordScreen(state: RecordingState, onIntent: (RecordIntent) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { RecordContent(state, onIntent) }
        SensorSelectionTopBar(state, onIntent)
    }
}
