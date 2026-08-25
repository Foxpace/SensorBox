package com.tomasrepcik.sensorbox.core.preferences

import com.tomasrepcik.sensorbox.core.testing.AppPreferencesFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreferencesReducerTest {
    @Test
    fun `Given fresh preferences When intro completes Then completion is retained`() {
        val givenPreferences = AppPreferencesFixtures.preferences()

        val actual = AppPreferencesReducer.reduce(
            current = givenPreferences,
            intent = AppPreferencesIntent.CompleteIntro,
        )

        assertTrue(actual.onboarding.hasCompletedIntro)
    }

    @Test
    fun `Given any preferences When invalid GPS interval is submitted Then it is clamped`() {
        val givenPreferences = AppPreferencesFixtures.preferences(gpsIntervalSeconds = 10)

        val actual = AppPreferencesReducer.reduce(
            current = givenPreferences,
            intent = AppPreferencesIntent.SetGpsInterval(seconds = 0),
        )

        assertEquals(1, actual.recording.gpsIntervalSeconds)
    }

    @Test
    fun `Given battery protection enabled When disabled Then the preference changes`() {
        val original = AppPreferencesFixtures.preferences()
        val givenPreferences = original.copy(
            recording = original.recording.copy(restrictMeasurementOnLowBattery = true),
        )

        val actual = AppPreferencesReducer.reduce(
            current = givenPreferences,
            intent = AppPreferencesIntent.SetLowBatteryRestriction(false),
        )

        assertEquals(false, actual.recording.restrictMeasurementOnLowBattery)
    }

    @Test
    fun `Given screen awake disabled When enabled Then the preference changes`() {
        val original = AppPreferencesFixtures.preferences()
        val givenPreferences = original.copy(display = original.display.copy(keepPhoneDisplayOn = false))

        val actual = AppPreferencesReducer.reduce(
            current = givenPreferences,
            intent = AppPreferencesIntent.SetKeepPhoneDisplayOn(true),
        )

        assertTrue(actual.display.keepPhoneDisplayOn)
    }

    @Test
    fun `Given automatic theme When dark theme is selected Then the preference changes`() {
        val givenPreferences = AppPreferencesFixtures.preferences()

        val actual = AppPreferencesReducer.reduce(
            current = givenPreferences,
            intent = AppPreferencesIntent.SetThemeMode(AppThemeMode.DARK),
        )

        assertEquals(AppThemeMode.DARK, actual.display.themeMode)
    }

    @Test
    fun `Given dynamic colors enabled When disabled Then the preference changes`() {
        val givenPreferences = AppPreferencesFixtures.preferences()

        val actual = AppPreferencesReducer.reduce(
            current = givenPreferences,
            intent = AppPreferencesIntent.SetDynamicColors(false),
        )

        assertEquals(false, actual.display.dynamicColors)
    }
}
