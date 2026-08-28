package com.tomasrepcik.sensorbox.domain.sensors

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchSensorCatalogStoreTest {
    @Test
    fun `Given an empty sensor response When catalog updates Then watch is available`() {
        val catalog = WatchSensorCatalogStore()

        catalog.update(emptyList())

        assertTrue(catalog.isAvailable.value)
    }

    @Test
    fun `Given an available watch app When catalog clears Then watch is unavailable`() {
        val catalog = WatchSensorCatalogStore().apply { update(emptyList()) }

        catalog.clear()

        assertFalse(catalog.isAvailable.value)
    }
}
