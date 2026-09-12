package com.tomasrepcik.sensorbox.home

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.tomasrepcik.sensorbox.design.WearSensorBoxTheme
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.hypot

class WearDashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenPhoneRecordingWhenDisplayedThenActualSourcesAndAccessibleStopAreShown() {
        // Given
        val state = WearDashboardState(
            route = WearRoute.ACTIVE,
            selectedSensorIds = setOf(1, 2, 3),
            includesGps = true,
            activeSession = RecordingSessionState.Running("phone", "walk", 0L, listOf(4), false, 60_000L, 3),
        )
        var received: WearDashboardIntent? = null
        composeRule.setContent {
            WearSensorBoxTheme { WearDashboardScreen(state) { received = it } }
        }

        // When
        val stop = composeRule.onNodeWithText("Stop").assertIsDisplayed()
        val bounds = stop.fetchSemanticsNode().boundsInRoot
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        saveScreen("watch-recording.png")
        stop.performClick()

        // Then
        composeRule.onNodeWithText("1 source").assertIsDisplayed()
        composeRule.onNodeWithText("GPS off").assertIsDisplayed()
        assertEquals(WearDashboardIntent.StopRecording, received)
        val radius = minOf(screen.width, screen.height) / 2
        listOf(bounds.topLeft, bounds.topRight, bounds.bottomLeft, bounds.bottomRight).forEach { corner ->
            assertTrue("Stop button crosses the round screen edge", hypot(
                corner.x - screen.center.x,
                corner.y - screen.center.y,
            ) < radius)
        }
    }
    @Test
    fun givenLargeTextWhenRecordingThenStopRemainsInsideTheRoundScreen() {
        // Given
        val state = WearDashboardState(
            route = WearRoute.ACTIVE,
            activeSession = RecordingSessionState.Running("phone", "walk", 0L, listOf(4), false),
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.3f)) {
                WearSensorBoxTheme { WearDashboardScreen(state) {} }
            }
        }

        // When
        val stop = composeRule.onNodeWithText("Stop").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val screen = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        saveScreen("watch-recording-large-text.png")

        // Then
        val radius = minOf(screen.width, screen.height) / 2
        listOf(stop.topLeft, stop.topRight, stop.bottomLeft, stop.bottomRight).forEach { corner ->
            assertTrue(hypot(corner.x - screen.center.x, corner.y - screen.center.y) < radius)
        }
    }

    private fun saveScreen(name: String) {
        val context: Context = ApplicationProvider.getApplicationContext()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }

}
