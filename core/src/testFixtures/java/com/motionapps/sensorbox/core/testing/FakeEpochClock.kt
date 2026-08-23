package com.motionapps.sensorbox.core.testing

import com.motionapps.sensorbox.core.time.EpochClock

class FakeEpochClock(var currentMillis: Long = 0L) : EpochClock {
    override fun nowMillis(): Long = currentMillis
}
