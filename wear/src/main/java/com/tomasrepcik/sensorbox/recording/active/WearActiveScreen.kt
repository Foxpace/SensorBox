package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import com.tomasrepcik.sensorbox.home.WearDashboardIntent
import com.tomasrepcik.sensorbox.home.WearDashboardState

@Composable
internal fun WearActiveScreen(state: WearDashboardState, accept: (WearDashboardIntent) -> Unit) {
    val selectedCount = state.selectedSensorIds.size + if (state.includesGps) 1 else 0
    AppScaffold {
        ScreenScaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WearRecordingStatus(
                    selectedCount = selectedCount,
                    keepsDisplayOn = state.preferences.display.keepWearDisplayOn,
                )
                Spacer(Modifier.height(22.dp))
                WearStopRecordingButton { accept(WearDashboardIntent.StopRecording) }
            }
        }
    }
}
