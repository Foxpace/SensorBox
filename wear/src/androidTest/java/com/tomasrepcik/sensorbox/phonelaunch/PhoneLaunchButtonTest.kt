package com.tomasrepcik.sensorbox.phonelaunch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.tomasrepcik.sensorbox.design.WearSensorBoxTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.hypot

class PhoneLaunchButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenRoundWatchWhenPhoneRequestIsSentThenButtonLabelFitsInsideDisplay() {
        // Given
        composeRule.setContent {
            WearSensorBoxTheme {
                PhoneLaunchScreen(PhoneLaunchState(true, PhoneLaunchStatus.SENT)) {}
            }
        }

        // When
        val label = composeRule.onNodeWithText("Open phone app").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot

        // Then
        val radius = minOf(screen.width, screen.height) / 2
        listOf(label.topLeft, label.topRight, label.bottomLeft, label.bottomRight).forEach { corner ->
            assertTrue(hypot(corner.x - screen.center.x, corner.y - screen.center.y) < radius)
        }
    }
}
