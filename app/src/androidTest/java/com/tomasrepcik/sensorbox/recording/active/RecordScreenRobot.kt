package com.tomasrepcik.sensorbox.recording.active

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tomasrepcik.sensorbox.design.SensorBoxTheme
import com.tomasrepcik.sensorbox.recording.RecordingIntent
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor

class RecordScreenRobot(private val rule: ComposeContentTestRule) {
    fun givenRecordScreen(
        selected: Boolean = false,
        gpsSelected: Boolean = false,
        watchConnected: Boolean = false,
        onIntent: (RecordingIntent) -> Unit = {},
    ) = apply {
        rule.setContent {
            SensorBoxTheme {
                RecordScreen(
                    state = RecordingState(
                        sensors = listOf(SensorDescriptor(1, "Accelerometer", "Fixture")),
                        selectedSensorIds = if (selected) setOf(1) else emptySet(),
                        includesGps = gpsSelected,
                        isWatchConnected = watchConnected,
                        watchSensors = if (watchConnected) {
                            listOf(SensorDescriptor(1, "Wear Accelerometer", "Fixture"))
                        } else {
                            emptyList()
                        },
                        recordingArchivePath = "Fixture/SensorBox",
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

    fun thenRecordingButtonIsVisible() = apply {
        rule.onNodeWithText("Continue").assertIsDisplayed()
    }
}
