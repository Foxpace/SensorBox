package com.tomasrepcik.sensorbox.recording.setup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tomasrepcik.sensorbox.design.SensorBoxTheme
import com.tomasrepcik.sensorbox.recording.RecordingIntent
import com.tomasrepcik.sensorbox.recording.RecordingMessage
import com.tomasrepcik.sensorbox.recording.RecordingMessageText
import com.tomasrepcik.sensorbox.recording.RecordingState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecordingSetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenConfiguredSetupWhenStartIsTappedThenRecordingRequestIsSent() {
        var actualIntent: RecordingIntent? = null
        RecordingSetupScreenRobot(composeRule)
            .givenRecordingSetup { actualIntent = it }
            .thenFolderAndSettingsAreVisible()
            .whenStartRecordingIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordingIntent.StartRecording, actualIntent)
        }
    }

    @Test
    fun givenWearPermissionFailureThenActionableWatchGuidanceIsDisplayed() {
        composeRule.setContent {
            SensorBoxTheme {
                RecordingMessageText(RecordingMessage.WATCH_PERMISSION_REQUIRED)
            }
        }

        composeRule.onNodeWithText("Permission required on Wear OS").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Open SensorBox on your watch, grant Location permission, then try again.",
        ).assertIsDisplayed()
    }

    @Test
    fun givenRecordingIsStartingThenProgressLabelIsDisplayed() {
        composeRule.setContent {
            SensorBoxTheme {
                RecordingSetupScreen(
                    state = RecordingState(
                        selectedSensorIds = setOf(1),
                        recordingArchivePath = "Fixture/SensorBox",
                        isStarting = true,
                    ),
                    onIntent = { },
                )
            }
        }

        composeRule.onNodeWithText("Starting recording…").assertIsDisplayed()
    }
}
