package com.tomasrepcik.sensorbox.domain.sensors

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearSensorCatalogStoreTest {
    @Test
    fun `Given an empty sensor response When catalog updates Then Wear is available`() {
        val catalog = WearSensorCatalogStore()

        catalog.update(emptyList())

        assertTrue(catalog.isAvailable.value)
    }

    @Test
    fun `Given an available Wear app When catalog clears Then Wear is unavailable`() {
        val catalog = WearSensorCatalogStore().apply { update(emptyList()) }

        catalog.clear()

        assertFalse(catalog.isAvailable.value)
    }
}
