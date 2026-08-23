package com.motionapps.sensorbox.presentation.main

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecordScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenRecordScreenWhenSensorIsTappedThenToggleIntentIsSent() {
        var actualIntent: MainIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen { actualIntent = it }
            .thenRecordingActionIsVisible()
            .whenAccelerometerIsTapped()

        composeRule.runOnIdle {
            assertEquals(MainIntent.ToggleSensor(1), actualIntent)
        }
    }

    @Test
    fun givenRecordScreenWhenSensorInfoIsTappedThenDetailsIntentIsSent() {
        var actualIntent: MainIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen { actualIntent = it }
            .whenAccelerometerInfoIsTapped()

        composeRule.runOnIdle {
            assertEquals(MainIntent.OpenSensorDetails(1), actualIntent)
        }
    }

    @Test
    fun givenSelectedSensorWhenContinueIsTappedThenSetupIntentIsSent() {
        var actualIntent: MainIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen(selected = true) { actualIntent = it }
            .whenContinueIsTapped()

        composeRule.runOnIdle {
            assertEquals(MainIntent.OpenMeasurementSetup, actualIntent)
        }
    }

    @Test
    fun givenRecordScreenWhenGpsIsTappedThenToggleGpsIntentIsSent() {
        var actualIntent: MainIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen { actualIntent = it }
            .whenGpsIsTapped()

        composeRule.runOnIdle {
            assertEquals(MainIntent.ToggleGps, actualIntent)
        }
    }

    @Test
    fun givenOnlyGpsSelectedWhenContinueIsTappedThenSetupIntentIsSent() {
        var actualIntent: MainIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen(gpsSelected = true) { actualIntent = it }
            .whenContinueIsTapped()

        composeRule.runOnIdle {
            assertEquals(MainIntent.OpenMeasurementSetup, actualIntent)
        }
    }
}
