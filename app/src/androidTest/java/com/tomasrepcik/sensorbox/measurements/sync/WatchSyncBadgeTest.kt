package com.tomasrepcik.sensorbox.measurements.sync

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomasrepcik.sensorbox.design.SensorBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WatchSyncBadgeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenOpenDialogWhenRefreshIsTappedThenOnlyMeasurementCountIsRequested() {
        // Given
        val state = WatchSyncState(status = WatchSyncStatus.AVAILABLE)
        val intents = mutableListOf<WatchSyncIntent>()
        composeRule.setContent { SensorBoxTheme { WatchSyncContent(state, true, intents::add) } }
        composeRule.onNodeWithContentDescription("Watch up to date. Nothing to sync").performClick()

        // When
        composeRule.onNodeWithContentDescription("Check watch again").assertIsDisplayed().performClick()

        // Then
        assertEquals(listOf(WatchSyncIntent.CHECK), intents)
    }

    @Test
    fun givenFailureWhenIconIsTappedThenIssueAndRetryAreShown() {
        // Given
        val state = WatchSyncState(status = WatchSyncStatus.FAILED, message = "Watch connection timed out")
        val intents = mutableListOf<WatchSyncIntent>()
        composeRule.setContent { SensorBoxTheme { WatchSyncContent(state, false, intents::add) } }

        // When
        composeRule.onNodeWithContentDescription("Sync failed · tap to retry").performClick()

        // Then
        composeRule.onNodeWithText("Watch connection timed out").assertIsDisplayed()
        assertTrue(intents.isEmpty())
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(listOf(WatchSyncIntent.RETRY), intents)
    }

    @Test
    fun givenAvailableMeasurementsWhenOpeningDialogThenOnlySyncButtonRequestsTransfer() {
        // Given
        val state = WatchSyncState(status = WatchSyncStatus.AVAILABLE, measurementCount = 2, fileCount = 15)
        val intents = mutableListOf<WatchSyncIntent>()
        composeRule.setContent { SensorBoxTheme { WatchSyncContent(state, true, intents::add) } }

        // When
        composeRule.onNodeWithContentDescription("Fetch 2 watch measurements").performClick()

        // Then
        composeRule.onNodeWithText("Sync from watch").assertIsDisplayed()
        assertTrue(intents.isEmpty())
        composeRule.onNodeWithText("Sync from watch").performClick()
        assertEquals(listOf(WatchSyncIntent.COPY), intents)
    }

    @Test
    fun givenActiveTransferWhenProgressIsClosedAndReopenedThenProgressRemainsVisibleWithoutAnotherRequest() {
        // Given
        val state = WatchSyncState(
            status = WatchSyncStatus.COPYING,
            measurementCount = 2,
            fileCount = 15,
            receivedFiles = listOf("WEAR_walk/accelerometer.csv"),
        )
        val intents = mutableListOf<WatchSyncIntent>()
        composeRule.setContent { SensorBoxTheme { WatchSyncContent(state, false, intents::add) } }

        // When
        composeRule.onNodeWithContentDescription("Watch syncing. Show progress").performClick()
        composeRule.onNodeWithText("1 of 15 files saved").assertIsDisplayed()
        composeRule.onNodeWithText("Close").performClick()
        composeRule.onNodeWithContentDescription("Watch syncing. Show progress").performClick()

        // Then
        composeRule.onNodeWithText("1 of 15 files saved").assertIsDisplayed()
        composeRule.onNodeWithText("WEAR_walk/accelerometer.csv").assertDoesNotExist()
        composeRule.onNodeWithText("Recording folder settings").assertDoesNotExist()
        assertTrue(intents.isEmpty())
    }

    @Test
    fun givenNoPendingMeasurementsWhenDisplayedThenWatchIsUpToDate() {
        // Given
        val state = WatchSyncState(status = WatchSyncStatus.AVAILABLE)

        // When
        composeRule.setContent { SensorBoxTheme { WatchSyncButton(state, true) {} } }

        // Then
        composeRule.onNodeWithContentDescription("Watch up to date. Nothing to sync").assertIsDisplayed()
        composeRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun givenTwoMeasurementsWithManyFilesWhenDisplayedThenBadgeShowsTwoAndOpensCopy() {
        // Given
        val state = WatchSyncState(status = WatchSyncStatus.AVAILABLE, measurementCount = 2, fileCount = 15)
        var opened = false
        composeRule.setContent { SensorBoxTheme { WatchSyncButton(state, true) { opened = true } } }

        // When
        composeRule.onNodeWithContentDescription("Fetch 2 watch measurements").assertIsDisplayed().performClick()

        // Then
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("15").assertDoesNotExist()
        assertTrue(opened)
    }

    @Test
    fun givenCompletedCopyWhenDisplayedThenWatchIsUpToDate() {
        // Given
        val state = WatchSyncState(status = WatchSyncStatus.COMPLETE, measurementCount = 2, fileCount = 15)

        // When
        composeRule.setContent { SensorBoxTheme { WatchSyncButton(state, true) {} } }

        // Then
        composeRule.onNodeWithContentDescription("Watch up to date. Nothing to sync").assertIsDisplayed()
    }
}
