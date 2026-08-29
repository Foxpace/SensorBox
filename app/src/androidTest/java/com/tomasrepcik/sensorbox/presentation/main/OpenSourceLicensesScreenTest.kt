package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tomasrepcik.sensorbox.ui.theme.SensorBoxTheme
import com.tomasrepcik.sensorbox.domain.licenses.OpenSourceLicense
import org.junit.Rule
import org.junit.Test

class OpenSourceLicensesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenGeneratedLicenseMetadataWhenLicenseIsOpenedThenItsTextIsShown() {
        composeRule.setContent {
            SensorBoxTheme {
                var state by remember {
                    mutableStateOf(
                        SettingsState(
                            openSourceLicenses = listOf(
                                OpenSourceLicense(
                                    "Debug License Info",
                                    "Licenses are only provided in build variants (e.g. release) where the Android " +
                                        "Gradle Plugin generates an app dependency list.",
                                ),
                            ),
                        ),
                    )
                }
                OpenSourceLicensesScreen(
                    state = state,
                    onIntent = { intent ->
                        if (intent is SettingsIntent.SelectOpenSourceLicense) {
                            state = state.copy(selectedLicenseName = intent.name)
                        }
                    },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Debug License Info").performClick()
        composeRule.onNodeWithText(
            "Licenses are only provided in build variants (e.g. release) where the Android Gradle Plugin " +
                "generates an app dependency list.",
        ).assertIsDisplayed()
    }
}
