package com.tomasrepcik.sensorbox.core.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Serializes starting sync with recording starts and archive selection. */
class MeasurementSyncLock {
    private val requests = mutableSetOf<String>()
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()

    @Synchronized
    fun begin(requestId: String) {
        requests.add(requestId)
        mutableBusy.value = true
    }

    @Synchronized
    fun finish(requestId: String) {
        requests.remove(requestId)
        mutableBusy.value = requests.isNotEmpty()
    }

    @Synchronized
    fun <T> whenIdle(operation: () -> T): T {
        check(requests.isEmpty()) { "Wait for watch sync to finish first" }
        return operation()
    }

    @Synchronized
    fun <T> whileActive(requestId: String, operation: () -> T): T {
        check(requestId in requests) { "Watch sync is no longer active" }
        return operation()
    }
}
