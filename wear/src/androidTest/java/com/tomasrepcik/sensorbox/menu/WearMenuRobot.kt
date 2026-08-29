package com.tomasrepcik.sensorbox.menu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.tomasrepcik.sensorbox.design.WearSensorBoxTheme

class WearMenuRobot(
    private val rule: ComposeContentTestRule,
) {
    fun givenMenu(onDestinationSelected: (WearMenuDestination) -> Unit = {}) = apply {
        rule.setContent {
            WearSensorBoxTheme {
                WearMenuScreen(WearMenuState(), onDestinationSelected)
            }
        }
    }

    fun whenRecordIsTapped() = apply {
        rule.onNodeWithContentDescription("Record").performClick()
    }

    fun whenSyncIsTapped() = apply {
        rule.onNodeWithContentDescription("Sync recordings").performClick()
    }

    fun thenRecordIsVisible() = apply {
        rule.onNodeWithContentDescription("Record").assertIsDisplayed()
    }

    fun thenSyncIsVisible() = apply {
        rule.onNodeWithContentDescription("Sync recordings").assertIsDisplayed()
    }
}
