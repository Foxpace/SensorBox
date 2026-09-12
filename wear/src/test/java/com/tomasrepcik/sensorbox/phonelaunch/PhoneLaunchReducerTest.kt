package com.tomasrepcik.sensorbox.phonelaunch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneLaunchReducerTest {
    @Test
    fun `Given a connected phone When launch is requested Then message sending starts`() {
        val givenState = PhoneLaunchState(isPhoneConnected = true)

        val whenNext = PhoneLaunchReducer.reduce(givenState, PhoneLaunchIntent.LaunchRequested)

        assertEquals(PhoneLaunchStatus.LAUNCHING, whenNext.state.status)
        assertEquals(PhoneLaunchEffect.SendLaunchMessage, whenNext.effect)
    }

    @Test
    fun `Given no connected phone When launch is requested Then unavailable is shown`() {
        val givenState = PhoneLaunchState(isPhoneConnected = false)

        val whenNext = PhoneLaunchReducer.reduce(givenState, PhoneLaunchIntent.LaunchRequested)

        assertEquals(PhoneLaunchStatus.PHONE_UNAVAILABLE, whenNext.state.status)
        assertNull(whenNext.effect)
    }
}
