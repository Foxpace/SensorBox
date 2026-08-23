package com.motionapps.sensorbox.core.format

import java.util.Locale

object ValueFormats {
    fun elapsedSeconds(totalSeconds: Long): String {
        val safeSeconds = totalSeconds.coerceAtLeast(0)
        val hours = safeSeconds / 3_600
        val minutes = safeSeconds % 3_600 / 60
        val seconds = safeSeconds % 60
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun decimal(value: Number, fractionDigits: Int = 2): String {
        require(fractionDigits in 0..MAX_FRACTION_DIGITS)
        return String.format(Locale.ROOT, "%.${fractionDigits}f", value.toDouble())
    }

    private const val MAX_FRACTION_DIGITS = 6
}
