package com.tomasrepcik.sensorbox.core.time

import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Instant

class ClockFormatsTest {
    @Test
    fun `Given a clock instant When a folder timestamp is formatted Then the requested zone is used`() {
        val millis = Instant.parse("2026-08-23T12:34:56Z").toEpochMilliseconds()

        assertEquals("2026-08-23_14-34-56", ClockFormats.folderTimestamp(millis, TimeZone.of("Europe/Bratislava")))
    }

    @Test
    fun `Given a clock instant When metadata is formatted Then the established shape is preserved`() {
        val millis = Instant.parse("2026-08-23T12:34:56Z").toEpochMilliseconds()

        assertEquals("23. 08. 2026 12:34:56", ClockFormats.metadataTimestamp(millis, TimeZone.UTC))
    }
}
