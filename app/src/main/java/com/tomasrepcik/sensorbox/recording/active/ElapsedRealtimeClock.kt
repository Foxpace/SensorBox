package com.tomasrepcik.sensorbox.recording.active

import android.os.SystemClock
import javax.inject.Inject

fun interface ElapsedRealtimeClock {
    fun nowMillis(): Long
}

class AndroidElapsedRealtimeClock @Inject constructor() : ElapsedRealtimeClock {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}
