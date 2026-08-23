package com.motionapps.sensorbox.core.preferences

import com.motionapps.sensorbox.core.testing.AppPreferencesFixtures
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

        assertTrue(actual.hasCompletedIntro)
    }

    @Test
    fun `Given any preferences When invalid GPS interval is submitted Then it is clamped`() {
        val givenPreferences = AppPreferencesFixtures.preferences(gpsIntervalSeconds = 10)

        val actual = AppPreferencesReducer.reduce(
            current = givenPreferences,
            intent = AppPreferencesIntent.SetGpsInterval(seconds = 0),
        )

        assertEquals(1, actual.gpsIntervalSeconds)
    }

    @Test
    fun `Given battery protection enabled When disabled Then the preference changes`() {
        val givenPreferences = AppPreferencesFixtures.preferences().copy(restrictMeasurementOnLowBattery = true)

        val actual = AppPreferencesReducer.reduce(
            current = givenPreferences,
            intent = AppPreferencesIntent.SetLowBatteryRestriction(false),
        )

        assertEquals(false, actual.restrictMeasurementOnLowBattery)
    }

    @Test
    fun `Given screen awake disabled When enabled Then the preference changes`() {
        val givenPreferences = AppPreferencesFixtures.preferences().copy(keepPhoneDisplayOn = false)

        val actual = AppPreferencesReducer.reduce(
            current = givenPreferences,
            intent = AppPreferencesIntent.SetKeepPhoneDisplayOn(true),
        )

        assertTrue(actual.keepPhoneDisplayOn)
    }
}
