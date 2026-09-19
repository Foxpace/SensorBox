package com.tomasrepcik.sensorbox.core.time

fun interface EpochClock {
    fun nowMillis(): Long
}

object SystemEpochClock : EpochClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
