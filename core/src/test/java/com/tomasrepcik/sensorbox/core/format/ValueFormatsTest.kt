package com.tomasrepcik.sensorbox.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ValueFormatsTest {
    @Test
    fun `Given zero and hour boundaries When formatted Then elapsed components are padded`() {
        assertEquals("00:00:00", ValueFormats.elapsedSeconds(0))
        assertEquals("00:59:59", ValueFormats.elapsedSeconds(3_599))
        assertEquals("01:00:00", ValueFormats.elapsedSeconds(3_600))
    }

    @Test
    fun `Given a duration over one day When formatted Then hours do not wrap`() {
        assertEquals("49:02:03", ValueFormats.elapsedSeconds(49 * 3_600L + 2 * 60L + 3))
    }

    @Test
    fun `Given a comma locale When a decimal is formatted Then output uses a period`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("12.50", ValueFormats.decimal(12.5))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
