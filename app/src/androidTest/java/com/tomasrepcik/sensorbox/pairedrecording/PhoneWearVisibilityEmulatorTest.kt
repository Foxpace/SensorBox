package com.tomasrepcik.sensorbox.pairedrecording

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tomasrepcik.sensorbox.bootstrap.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhoneWearVisibilityEmulatorTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun givenPairedWearAppWhenSourcesOpenThenWearSectionIsVisible() {
        compose.waitUntil(RESPONSE_TIMEOUT_MILLIS) {
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Wear OS"))
        compose.onNode(hasText("Wear OS")).assertExists()
    }

    private companion object {
        const val RESPONSE_TIMEOUT_MILLIS = 15_000L
    }
}
