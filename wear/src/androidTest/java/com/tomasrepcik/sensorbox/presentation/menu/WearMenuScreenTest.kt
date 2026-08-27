package com.tomasrepcik.sensorbox.presentation.menu

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
    fun givenMenuWhenSyncIsTappedThenSyncDestinationIsSelected() {
        var selectedDestination: WearMenuDestination? = null
        WearMenuRobot(composeRule)
            .givenMenu { selectedDestination = it }
            .thenSyncIsVisible()
            .whenSyncIsTapped()

        composeRule.runOnIdle {
            assertEquals(WearMenuDestination.SYNC, selectedDestination)
        }
    }
}
