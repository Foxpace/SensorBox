package com.motionapps.sensorbox.presentation.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.motionapps.sensorbox.ui.theme.SensorBoxTheme

class OnboardingScreenRobot(private val rule: ComposeContentTestRule) {
    var lastIntent: MainIntent? = null
        private set

    fun givenInteractiveOnboarding(page: Int = 0, storagePath: String? = null) = apply {
        rule.setContent {
            var state by remember {
                mutableStateOf(
                    MainState(route = MainRoute.ONBOARDING, onboardingPage = page, storagePath = storagePath),
                )
            }
            SensorBoxTheme {
                OnboardingScreen(state) { intent ->
                    lastIntent = intent
                    state = MainReducer.reduce(state, intent).state
                }
            }
        }
    }

    fun thenPageIsVisible(title: String) = apply {
        rule.onNodeWithText(title).assertIsDisplayed()
        rule.onNodeWithContentDescription(title).assertIsDisplayed()
    }

    fun whenNextIsTapped() = apply {
        rule.onNodeWithText("Next").performClick()
    }

    fun whenPrivacyPolicyIsTapped() = apply {
        rule.onNodeWithText("Privacy policy").performClick()
    }

    fun whenTermsOfUseIsTapped() = apply {
        rule.onNodeWithText("Terms of use").performClick()
    }

    fun whenBatterySettingsIsTapped() = apply {
        rule.onNodeWithText("Review battery settings").performClick()
    }

    fun whenChooseFolderIsTapped() = apply {
        rule.onNodeWithText("Choose folder").performClick()
    }

    fun whenFinishIsTapped() = apply {
        rule.onNodeWithText("Start SensorBox").performClick()
    }

    fun thenFinishIsDisabled() = apply {
        rule.onNodeWithText("Start SensorBox").assertIsNotEnabled()
    }

    fun thenFinishIsEnabled() = apply {
        rule.onNodeWithText("Start SensorBox").assertIsEnabled()
    }
}
