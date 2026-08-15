package com.motionapps.sensorbox.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
import com.motionapps.sensorbox.ui.theme.SensorBoxTheme

class RecordScreenRobot(private val rule: ComposeContentTestRule) {
    fun givenRecordScreen(
        selected: Boolean = false,
        gpsSelected: Boolean = false,
        onIntent: (MainIntent) -> Unit = {},
    ) = apply {
        rule.setContent {
            SensorBoxTheme {
                RecordScreen(
                    state = MainState(
                        route = MainRoute.RECORD,
                        sensors = listOf(SensorDescriptor(1, "Accelerometer", "Fixture", false)),
                        selectedSensorIds = if (selected) setOf(1) else emptySet(),
                        includesGps = gpsSelected,
                        storagePath = "Fixture/SensorBox",
                    ),
                    onIntent = onIntent,
                )
            }
        }
    }

    fun whenAccelerometerIsTapped() = apply {
        rule.onNodeWithText("Accelerometer").performClick()
    }

    fun whenAccelerometerInfoIsTapped() = apply {
        rule.onNodeWithContentDescription("Information about Accelerometer").performClick()
    }

    fun whenGpsIsTapped() = apply {
        rule.onNodeWithText("GPS").performClick()
    }

    fun whenContinueIsTapped() = apply {
        rule.onNodeWithText("Continue").performClick()
    }

    fun thenRecordingActionIsVisible() = apply {
        rule.onNodeWithText("Continue").assertIsDisplayed()
    }
}
