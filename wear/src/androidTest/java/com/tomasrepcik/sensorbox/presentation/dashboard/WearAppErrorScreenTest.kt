package com.tomasrepcik.sensorbox.presentation.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WearAppErrorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenStorageFailureWhenRenderedThenLocalizedMessageCanBeDismissed() {
        // Given
        var dismissed = false

        // When
        composeRule.setContent {
            WearAppErrorScreen(AppErrorCode.STORAGE) { dismissed = true }
        }

        // Then
        composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeRule.onNodeWithText("Local storage could not be used. The error was saved.").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        assertTrue(dismissed)
    }
}
