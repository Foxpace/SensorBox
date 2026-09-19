package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.tomasrepcik.sensorbox.recording.RecordingDevice
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecordScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenRecordScreenWhenSensorIsTappedThenToggleIntentIsSent() {
        var actualIntent: RecordIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen { actualIntent = it }
            .thenRecordingButtonIsVisible()
            .whenAccelerometerIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordIntent.ToggleSensor(1), actualIntent)
        }
    }

    @Test
    fun givenRecordScreenWhenSensorInfoIsTappedThenDetailsIntentIsSent() {
        var actualIntent: RecordIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen { actualIntent = it }
            .whenAccelerometerInfoIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordIntent.OpenSensorDetails(1), actualIntent)
        }
    }

    @Test
    fun givenSelectedSensorWhenContinueIsTappedThenSetupIntentIsSent() {
        var actualIntent: RecordIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen(selected = true) { actualIntent = it }
            .whenContinueIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordIntent.OpenRecordingSetup, actualIntent)
        }
    }

    @Test
    fun givenRecordScreenWhenGpsIsTappedThenToggleGpsIntentIsSent() {
        var actualIntent: RecordIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen { actualIntent = it }
            .whenGpsIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordIntent.ToggleGps, actualIntent)
        }
    }

    @Test
    fun givenWearSensorWhenInfoIsTappedThenWearDetailsIntentIsSent() {
        var actualIntent: RecordIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen(watchConnected = true) { actualIntent = it }
            .whenWearAccelerometerInfoIsTapped()

        composeRule.runOnIdle {
            assertEquals(
                RecordIntent.OpenSensorDetails(1, RecordingDevice.WATCH),
                actualIntent,
            )
        }
    }

    @Test
    fun givenOnlyGpsSelectedWhenContinueIsTappedThenSetupIntentIsSent() {
        var actualIntent: RecordIntent? = null
        RecordScreenRobot(composeRule)
            .givenRecordScreen(gpsSelected = true) { actualIntent = it }
            .whenContinueIsTapped()

        composeRule.runOnIdle {
            assertEquals(RecordIntent.OpenRecordingSetup, actualIntent)
        }
    }
}
