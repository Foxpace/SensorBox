package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun RecordingSetupScreen(
    state: RecordingState,
    onIntent: (RecordingIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { onIntent(RecordingIntent.ReturnToSensorSelection) },
) {
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { RecordingSetupContent(state, onIntent, onBack) }
        RecordingSetupTopBar(state, onIntent)
    }
}
