package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tomasrepcik.sensorbox.ui.theme.SensorBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MeasurementSetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenConfiguredSetupWhenStartIsTappedThenMeasurementIntentIsSent() {
        var actualIntent: RecordingIntent? = null
        MeasurementSetupScreenRobot(composeRule)
            .givenMeasurementSetup { actualIntent = it }
            .thenFolderAndSettingsAreVisible()
            .whenStartMeasurementIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordingIntent.StartMeasurement, actualIntent)
        }
    }

    @Test
    fun givenWearPermissionFailureThenActionableWatchGuidanceIsDisplayed() {
        composeRule.setContent {
            SensorBoxTheme {
                RecordingMessageText(RecordingMessage.WEAR_PERMISSION_REQUIRED)
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
                MeasurementSetupScreen(
                    state = RecordingState(
                        selectedSensorIds = setOf(1),
                        storagePath = "Fixture/SensorBox",
                        isStarting = true,
                    ),
                    onIntent = { },
                )
            }
        }

        composeRule.onNodeWithText("Starting recording…").assertIsDisplayed()
    }
}
