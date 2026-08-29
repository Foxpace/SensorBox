package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.design.SensorBoxSettingsDivider
import com.tomasrepcik.sensorbox.design.SensorBoxSettingsSection
import com.tomasrepcik.sensorbox.recording.RecordingIntent
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState

@Composable
fun ActiveRecordingScreen(state: RecordingState, onIntent: (RecordingIntent) -> Unit, modifier: Modifier = Modifier) {
    val session = state.session as? RecordingSessionState.Running ?: return
    Column(modifier.fillMaxSize()) {
        RecordingHeader(Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            RecordingTimer(state.elapsedSeconds, session.folderName)
            SensorBoxSettingsDivider()
            RecordingSummary(state, session)
            SensorBoxSettingsSection(stringResource(R.string.recording_annotations_category))
            AnnotationEditor(onIntent)
            Spacer(Modifier.height(24.dp))
        }
        ActiveRecordingTopBar(onIntent)
    }
}
