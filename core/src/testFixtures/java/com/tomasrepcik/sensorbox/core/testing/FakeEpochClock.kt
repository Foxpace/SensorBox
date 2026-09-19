package com.tomasrepcik.sensorbox.core.testing

import com.tomasrepcik.sensorbox.core.time.EpochClock

class FakeEpochClock(var currentMillis: Long = 0L) : EpochClock {
    override fun nowMillis(): Long = currentMillis
}
