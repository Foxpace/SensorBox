package com.tomasrepcik.sensorbox.design

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.wear.compose.material3.LocalContentColor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WearSystemThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenAutomaticThemeWhenSystemChangesThenBackgroundAndTextFollowIt() {
        // Given
        val night = mutableStateOf(false)
        var textColor = Color.Transparent
        composeRule.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = if (night.value) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                WearSensorBoxTheme(dynamicColor = false) { textColor = LocalContentColor.current }
            }
        }
        composeRule.waitForIdle()
        assertEquals(Color.White, composeRule.onRoot().captureToImage().toPixelMap()[50, 50])
        assertEquals(Color.Black, textColor)

        // When
        composeRule.runOnIdle { night.value = true }
        composeRule.waitForIdle()

        // Then
        assertEquals(Color.Black, composeRule.onRoot().captureToImage().toPixelMap()[50, 50])
        assertEquals(Color.White, textColor)
    }
}
