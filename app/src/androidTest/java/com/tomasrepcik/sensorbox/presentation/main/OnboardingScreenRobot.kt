package com.tomasrepcik.sensorbox.presentation.main

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
import com.tomasrepcik.sensorbox.ui.theme.SensorBoxTheme

class OnboardingScreenRobot(private val rule: ComposeContentTestRule) {
    var lastIntent: OnboardingIntent? = null
        private set

    fun givenInteractiveOnboarding(page: Int = 0, recordingArchivePath: String? = null) = apply {
        rule.setContent {
            var state by remember {
                mutableStateOf(OnboardingState(page = page, recordingArchivePath = recordingArchivePath))
            }
            SensorBoxTheme {
                OnboardingScreen(state) { intent ->
                    lastIntent = intent
                    state = when (intent) {
                        OnboardingIntent.AdvanceOnboarding -> state.copy(page = state.page + 1)
                        OnboardingIntent.RetreatOnboarding -> state.copy(page = (state.page - 1).coerceAtLeast(0))
                        else -> state
                    }
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
        rule.onNodeWithText("Choose archive").performClick()
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
