package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.ui.test.junit4.v2.createComposeRule
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
}
