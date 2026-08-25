package com.tomasrepcik.sensorbox.core.time

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object ClockFormats {
    fun folderTimestamp(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
        FOLDER_TIMESTAMP.format(localDateTime(epochMillis, timeZone))

    fun metadataTimestamp(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
        METADATA_TIMESTAMP.format(localDateTime(epochMillis, timeZone))

    fun diagnosticTimestamp(epochMillis: Long): String = Instant.fromEpochMilliseconds(epochMillis).toString()

    private fun localDateTime(epochMillis: Long, timeZone: TimeZone): LocalDateTime =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)

    private val FOLDER_TIMESTAMP = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
        char('_')
        hour()
        char('-')
        minute()
        char('-')
        second()
    }

    private val METADATA_TIMESTAMP = LocalDateTime.Format {
        day()
        chars(". ")
        monthNumber()
        chars(". ")
        year()
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
    }
}
