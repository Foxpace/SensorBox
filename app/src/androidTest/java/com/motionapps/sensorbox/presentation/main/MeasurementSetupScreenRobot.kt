package com.motionapps.sensorbox.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
import com.motionapps.sensorbox.ui.theme.SensorBoxTheme

class MeasurementSetupScreenRobot(private val rule: ComposeContentTestRule) {
    fun givenMeasurementSetup(onIntent: (MainIntent) -> Unit = {}) = apply {
        rule.setContent {
            SensorBoxTheme {
                MeasurementSetupScreen(
                    state = MainState(
                        route = MainRoute.SETUP,
                        sensors = listOf(SensorDescriptor(1, "Accelerometer", "Fixture", false)),
                        selectedSensorIds = setOf(1),
                        storagePath = "Fixture/SensorBox",
                    ),
                    onIntent = onIntent,
                )
            }
        }
    }

    fun whenStartMeasurementIsTapped() = apply {
        rule.onNodeWithText("Start measurement").performClick()
    }

    fun thenFolderAndSettingsAreVisible() = apply {
        rule.onNodeWithText("Recording folder").assertIsDisplayed()
        rule.onNodeWithText("Sensor sampling").assertIsDisplayed()
    }
}
