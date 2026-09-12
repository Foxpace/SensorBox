package com.tomasrepcik.sensorbox.menu

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WearMenuScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenMenuWhenRecordIsTappedThenRecordDestinationIsSelected() {
        var selectedDestination: WearMenuDestination? = null
        WearMenuRobot(composeRule)
            .givenMenu { selectedDestination = it }
            .thenRecordIsVisible()
            .whenRecordIsTapped()

        composeRule.runOnIdle {
            assertEquals(WearMenuDestination.RECORD, selectedDestination)
        }
    }

    @Test
    fun givenMenuWhenDisplayedThenSyncIsNotOffered() {
        // Given
        WearMenuRobot(composeRule).givenMenu {}

        // Then
        composeRule.onNodeWithText("Sync recordings").assertDoesNotExist()
        composeRule.onNodeWithText("Sync to phone").assertDoesNotExist()
    }
}
