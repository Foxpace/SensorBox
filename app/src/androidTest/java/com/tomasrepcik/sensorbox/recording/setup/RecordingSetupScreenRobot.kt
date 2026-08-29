package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomasrepcik.sensorbox.design.SensorBoxTheme
import com.tomasrepcik.sensorbox.recording.RecordingIntent
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor

class RecordingSetupScreenRobot(private val rule: ComposeContentTestRule) {
    fun givenRecordingSetup(onIntent: (RecordingIntent) -> Unit = {}) = apply {
        rule.setContent {
            SensorBoxTheme {
                RecordingSetupScreen(
                    state = RecordingState(
                        sensors = listOf(SensorDescriptor(1, "Accelerometer", "Fixture")),
                        selectedSensorIds = setOf(1),
                        recordingArchivePath = "Fixture/SensorBox",
                    ),
                    onIntent = onIntent,
                )
            }
        }
    }

    fun whenStartRecordingIsTapped() = apply {
        rule.onNodeWithText("Start recording").performClick()
    }

    fun thenFolderAndSettingsAreVisible() = apply {
        rule.onNodeWithText("Recording archive").assertIsDisplayed()
        rule.onNodeWithText("Sensor sampling").assertIsDisplayed()
    }
}
