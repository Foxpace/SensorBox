package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor
import com.tomasrepcik.sensorbox.ui.theme.SensorBoxTheme

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
