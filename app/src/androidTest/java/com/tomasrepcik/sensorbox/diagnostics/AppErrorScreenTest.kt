package com.tomasrepcik.sensorbox.diagnostics

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.design.SensorBoxTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppErrorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenStorageFailureWhenRenderedThenLocalizedMessageCanBeDismissed() {
        // Given
        var dismissed = false

        // When
        composeRule.setContent {
            SensorBoxTheme {
                AppErrorScreen(AppErrorCode.STORAGE) { dismissed = true }
            }
        }

        // Then
        composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Local storage could not be read or updated. The error was saved in diagnostics.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        assertTrue(dismissed)
    }
}
