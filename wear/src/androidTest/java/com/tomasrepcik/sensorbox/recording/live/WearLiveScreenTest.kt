package com.tomasrepcik.sensorbox.recording.live

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.tomasrepcik.sensorbox.design.WearSensorBoxTheme
import com.tomasrepcik.sensorbox.home.WearDashboardState
import com.tomasrepcik.sensorbox.recording.sources.WatchSensorDescriptor
import org.junit.Rule
import org.junit.Test

class WearLiveScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenNoSamplesWhenPreviewStartsAndSensorsChangeThenChartDoesNotCrash() {
        // Given
        val sensor = WatchSensorDescriptor(1, "Accelerometer", "Test", 1, "sensor", 10f, 1f, 1f, 0, 0, 0, false)
        val other = sensor.copy(type = 4, name = "Gyroscope")
        val state = mutableStateOf(WearDashboardState(sensors = listOf(sensor, other), liveSensorType = 1))
        composeRule.setContent { WearSensorBoxTheme { WearLiveScreen(state.value) {} } }
        composeRule.waitForIdle()

        // When
        composeRule.runOnIdle { state.value = state.value.copy(liveSamples = listOf(listOf(1f, 2f, 3f), listOf(2f, 3f, 4f))) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("X:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Y:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Z:", substring = true).assertIsDisplayed()
        composeRule.runOnIdle { state.value = state.value.copy(liveSensorType = 4, liveSamples = emptyList()) }
        composeRule.waitForIdle()

        // Then
        composeRule.onNodeWithText("Gyroscope").assertIsDisplayed()
    }
}
