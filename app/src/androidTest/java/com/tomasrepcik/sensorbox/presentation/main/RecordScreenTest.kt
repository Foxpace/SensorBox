package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecordScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenRecordScreenWhenSensorIsTappedThenToggleIntentIsSent() {
        var actualIntent: RecordingIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen { actualIntent = it }
            .thenRecordingActionIsVisible()
            .whenAccelerometerIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordingIntent.ToggleSensor(1), actualIntent)
        }
    }

    @Test
    fun givenRecordScreenWhenSensorInfoIsTappedThenDetailsIntentIsSent() {
        var actualIntent: RecordingIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen { actualIntent = it }
            .whenAccelerometerInfoIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordingIntent.OpenSensorDetails(1), actualIntent)
        }
    }

    @Test
    fun givenSelectedSensorWhenContinueIsTappedThenSetupIntentIsSent() {
        var actualIntent: RecordingIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen(selected = true) { actualIntent = it }
            .whenContinueIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordingIntent.OpenMeasurementSetup, actualIntent)
        }
    }

    @Test
    fun givenRecordScreenWhenGpsIsTappedThenToggleGpsIntentIsSent() {
        var actualIntent: RecordingIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen { actualIntent = it }
            .whenGpsIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordingIntent.ToggleGps, actualIntent)
        }
    }

    @Test
    fun givenOnlyGpsSelectedWhenContinueIsTappedThenSetupIntentIsSent() {
        var actualIntent: RecordingIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen(gpsSelected = true) { actualIntent = it }
            .whenContinueIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordingIntent.OpenMeasurementSetup, actualIntent)
        }
    }
}
