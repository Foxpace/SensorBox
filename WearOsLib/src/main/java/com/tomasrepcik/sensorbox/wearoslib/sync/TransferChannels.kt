package com.tomasrepcik.sensorbox.wearoslib.sync

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

/** Owns channel closure even when the transfer coroutine is blocked in stream I/O. */
internal class TransferChannels<T>(private val timeoutMillis: Long = 60_000L, private val close: (T) -> Unit) {
    private val active = mutableMapOf<T, String>()
    private val cancelled = LinkedHashSet<String>()

    @Synchronized
    fun cancel(requestId: String) {
        cancelled.add(requestId)
        if (cancelled.size > 128) cancelled.remove(cancelled.first())
        active.filterValues { it == requestId }.keys.toList().forEach(close)
    }

    suspend fun <R> use(requestId: String, channel: T, closeAfterSuccess: Boolean, work: suspend () -> R): R {
        synchronized(this) {
            if (requestId in cancelled) {
                close(channel)
                error("Watch transfer was cancelled")
            }
            active[channel] = requestId
        }
        return coroutineScope {
            val finished = AtomicBoolean(false)
            val expired = AtomicBoolean(false)
            val deadline = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                try {
                    delay(timeoutMillis.milliseconds)
                    expired.set(true)
                } finally {
                    if (!finished.get()) close(channel)
                }
            }
            try {
                val result = work()
                check(!expired.get()) { "Watch transfer timed out" }
                synchronized(this@TransferChannels) {
                    check(requestId !in cancelled) { "Watch transfer was cancelled" }
                }
                finished.set(true)
                result
            } finally {
                deadline.cancel()
                if (!finished.get() || closeAfterSuccess) close(channel)
                synchronized(this@TransferChannels) { active.remove(channel) }
            }
        }
    }
}
