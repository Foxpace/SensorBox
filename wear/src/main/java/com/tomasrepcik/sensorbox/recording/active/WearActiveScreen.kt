package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.MaterialTheme
import com.tomasrepcik.sensorbox.home.WearDashboardIntent
import com.tomasrepcik.sensorbox.home.WearDashboardState

@Composable
internal fun WearActiveScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = maxHeight * 0.14f)
                .fillMaxWidth(0.70f)
                .height(maxHeight * 0.35f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WearRecordingStatus(state)
        }
        WearStopRecordingButton(
            isStopping = state.isStopping,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = maxHeight * 0.20f)
                .fillMaxWidth(0.68f),
        ) { accept(WearDashboardIntent.StopRecording) }
    }
}
