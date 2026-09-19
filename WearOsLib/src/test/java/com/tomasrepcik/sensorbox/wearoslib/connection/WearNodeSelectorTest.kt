package com.tomasrepcik.sensorbox.wearoslib.connection

import org.junit.Assert.assertEquals
import org.junit.Test

class WearNodeSelectorTest {
    @Test
    fun `Given nearby and remote nodes When selecting Then nearby node is returned`() {
        val givenNearby = WearNode("nearby", "Watch", isNearby = true)
        val givenRemote = WearNode("remote", "Watch backup", isNearby = false)

        val whenSelected = WearNodeSelector.select(listOf(givenRemote, givenNearby))

        assertEquals(givenNearby, whenSelected)
    }

    @Test
    fun `Given remote nodes When selecting Then deterministic node is returned`() {
        val givenNodes = listOf(
            WearNode("z-node", "Second", isNearby = false),
            WearNode("a-node", "First", isNearby = false),
        )

        val whenSelected = WearNodeSelector.select(givenNodes)

        assertEquals("a-node", whenSelected?.id)
    }
}
