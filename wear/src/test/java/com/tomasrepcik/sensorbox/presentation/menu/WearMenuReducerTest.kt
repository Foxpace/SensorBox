package com.tomasrepcik.sensorbox.presentation.menu

import org.junit.Assert.assertEquals
import org.junit.Test

class WearMenuReducerTest {
    @Test
    fun `Given the menu When record is selected Then opening record is requested`() {
        val givenState = WearMenuState()

        val whenNext = WearMenuReducer.reduce(
            givenState,
            WearMenuIntent.SelectDestination(WearMenuDestination.RECORD),
        )

        assertEquals(givenState, whenNext.state)
        assertEquals(
            WearMenuEffect.OpenDestination(WearMenuDestination.RECORD),
            whenNext.effect,
        )
    }
}
