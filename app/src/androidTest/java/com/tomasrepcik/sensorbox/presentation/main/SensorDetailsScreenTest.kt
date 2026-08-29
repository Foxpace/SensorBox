package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor
import com.tomasrepcik.sensorbox.domain.sensors.SensorReportingMode
import com.tomasrepcik.sensorbox.ui.theme.SensorBoxTheme
import org.junit.Rule
import org.junit.Test

class SensorDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenWearSensorThenStaticParametersAreShownWithoutLivePreview() {
        composeRule.setContent {
            SensorBoxTheme {
                SensorDetailsScreen(
                    state = RecordingState(
                        watchSensors = listOf(
                            SensorDescriptor(
                                type = 1,
                                name = "Wear Accelerometer",
                                vendor = "Fixture Wear Vendor",
                                version = 7,
                                stringType = "fixture.sensor.accelerometer",
                                maximumRange = 78.4f,
                                resolution = 0.0024f,
                                power = 0.25f,
                                minimumDelayMicros = 5_000,
                                maximumDelayMicros = 200_000,
                                reportingMode = SensorReportingMode.CONTINUOUS,
                                isWakeUpSensor = true,
                            ),
                        ),
                        detailsSensorType = 1,
                        detailsDevice = RecordingDevice.WATCH,
                    ),
                    onBack = {},
                    onPreview = {},
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText("Wear Accelerometer").assertIsDisplayed()
        composeRule.onNodeWithText("Fixture Wear Vendor").assertIsDisplayed()
        listOf(
            "Version",
            "Resolution",
            "Power",
            "Maximum range",
            "Minimum delay",
            "Maximum delay",
            "Android sensor type",
            "Reporting mode",
            "Wake-up sensor",
        ).forEach { label ->
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(label, substring = true))
            composeRule.onNodeWithText(label, substring = true).assertIsDisplayed()
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("fixture.sensor.accelerometer"))
        composeRule.onNodeWithText("fixture.sensor.accelerometer").assertIsDisplayed()
        composeRule.onNodeWithText("Preview").assertDoesNotExist()
    }
}
