package com.motionapps.sensorbox.presentation.menu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.motionapps.sensorbox.ui.theme.WearSensorBoxTheme

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

    fun thenRecordIsVisible() = apply {
        rule.onNodeWithContentDescription("Record").assertIsDisplayed()
    }
}
