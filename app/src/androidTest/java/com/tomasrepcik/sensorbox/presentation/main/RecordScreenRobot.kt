package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor
import com.tomasrepcik.sensorbox.ui.theme.SensorBoxTheme

class RecordScreenRobot(private val rule: ComposeContentTestRule) {
    fun givenRecordScreen(
        selected: Boolean = false,
        gpsSelected: Boolean = false,
        wearConnected: Boolean = false,
        onIntent: (RecordingIntent) -> Unit = {},
    ) = apply {
        rule.setContent {
            SensorBoxTheme {
                RecordScreen(
                    state = RecordingState(
                        sensors = listOf(SensorDescriptor(1, "Accelerometer", "Fixture")),
                        selectedSensorIds = if (selected) setOf(1) else emptySet(),
                        includesGps = gpsSelected,
                        isWearConnected = wearConnected,
                        wearSensors = if (wearConnected) {
                            listOf(SensorDescriptor(1, "Wear Accelerometer", "Fixture"))
                        } else {
                            emptyList()
                        },
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

    fun whenWearAccelerometerInfoIsTapped() = apply {
        rule.onNodeWithText("Wear Accelerometer").performScrollTo()
        rule.onNodeWithContentDescription("Information about Wear Accelerometer").performClick()
    }

    fun whenContinueIsTapped() = apply {
        rule.onNodeWithText("Continue").performClick()
    }

    fun thenRecordingActionIsVisible() = apply {
        rule.onNodeWithText("Continue").assertIsDisplayed()
    }
}
