package com.tomasrepcik.sensorbox.measurements.list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomasrepcik.sensorbox.design.SensorBoxTheme
import com.tomasrepcik.sensorbox.measurements.components.measurementArchiveItems
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RecordingGroupsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenLinkedMeasurementsWhenWatchIsTappedThenWatchDetailsOpenWithinTheSharedRecording() {
        // Given
        val phone = MeasurementSummary("walk", "walk", 100L, "Today", 1, "session", "phone", "Morning walk")
        val watch = phone.copy(id = "WEAR_walk", device = "watch")
        val state = MeasurementsState(listOf(phone, watch))
        val intents = mutableListOf<MeasurementsIntent>()
        composeRule.setContent {
            SensorBoxTheme { LazyColumn { measurementArchiveItems(state, intents::add) } }
        }

        // When
        composeRule.onNodeWithText("Morning walk").assertIsDisplayed()
        composeRule.onNodeWithText("Phone + Watch · Paired recording").assertIsDisplayed()
        composeRule.onNodeWithText("Phone").assertIsDisplayed()
        composeRule.onNodeWithText("Watch").performClick()

        // Then
        assertEquals(listOf(MeasurementsIntent.OpenDetails("WEAR_walk")), intents)
    }
}
