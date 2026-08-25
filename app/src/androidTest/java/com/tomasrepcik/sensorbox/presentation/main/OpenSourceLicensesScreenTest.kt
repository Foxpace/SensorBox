package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tomasrepcik.sensorbox.ui.theme.SensorBoxTheme
import org.junit.Rule
import org.junit.Test

class OpenSourceLicensesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenGeneratedLicenseMetadataWhenLicenseIsOpenedThenItsTextIsShown() {
        composeRule.setContent {
            SensorBoxTheme {
                OpenSourceLicensesScreen(onBack = {})
            }
        }

        composeRule.onNodeWithText("Debug License Info").performClick()
        composeRule.onNodeWithText(
            "Licenses are only provided in build variants (e.g. release) where the Android Gradle Plugin " +
                "generates an app dependency list.",
        ).assertIsDisplayed()
    }
}
