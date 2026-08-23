package com.motionapps.sensorbox.presentation.main

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenFirstLaunchWhenAdvancingThenEveryHistoricalIntroductionScreenIsShown() {
        val robot = OnboardingScreenRobot(composeRule).givenInteractiveOnboarding()

        robot.thenPageIsVisible("Welcome to SensorBox").whenNextIsTapped()
        robot.thenPageIsVisible("Nothing is going out").whenNextIsTapped()
        robot.thenPageIsVisible("Privacy and terms").whenPrivacyPolicyIsTapped()
        assertEquals(MainIntent.OpenPrivacyPolicy, robot.lastIntent)
        robot.whenTermsOfUseIsTapped()
        assertEquals(MainIntent.OpenTermsOfUse, robot.lastIntent)
        robot.whenNextIsTapped().thenPageIsVisible("Android may pause recordings").whenNextIsTapped()
        robot.thenPageIsVisible("Allow reliable background work").whenBatterySettingsIsTapped()
        assertEquals(MainIntent.RequestBatteryOptimizationExemption, robot.lastIntent)
        robot.whenNextIsTapped().thenPageIsVisible("Choose a recording folder")
        robot.thenFinishIsDisabled().whenChooseFolderIsTapped()
        assertEquals(MainIntent.ChooseStorage, robot.lastIntent)
    }

    @Test
    fun givenSelectedFolderWhenFinishingThenCompleteIntroductionIntentIsSent() {
        val robot = OnboardingScreenRobot(composeRule).givenInteractiveOnboarding(
            page = 5,
            storagePath = "Documents/SensorBox",
        )

        robot.thenFinishIsEnabled().whenFinishIsTapped()

        assertEquals(MainIntent.CompleteOnboarding, robot.lastIntent)
    }
}
