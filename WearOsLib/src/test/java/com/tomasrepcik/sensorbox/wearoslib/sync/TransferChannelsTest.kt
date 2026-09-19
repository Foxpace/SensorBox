package com.tomasrepcik.sensorbox.wearoslib.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TransferChannelsTest {
    @Test
    fun `Given blocked channel input When the request is cancelled Then the stream closes and work exits`() =
        runBlocking {
            // Given
            val fixture = BlockingChannelFixture()
            val job = async(Dispatchers.IO) { runCatching { fixture.transfer() } }
            assertTrue(fixture.started.await(5, TimeUnit.SECONDS))

            // When
            fixture.channels.cancel("request")

            // Then
            assertTrue(job.await().isFailure)
            assertTrue(fixture.closed.await(5, TimeUnit.SECONDS))
        }

    @Test
    fun `Given blocked channel input When the owner is cancelled Then channel closure unblocks the child`() =
        runBlocking {
            // Given
            val fixture = BlockingChannelFixture()
            val job = launch(Dispatchers.IO) { fixture.transfer() }
            assertTrue(fixture.started.await(5, TimeUnit.SECONDS))

            // When
            job.cancelAndJoin()

            // Then
            assertTrue(fixture.closed.await(5, TimeUnit.SECONDS))
            assertTrue(job.isCompleted)
        }

    @Test
    fun `Given a stalled channel When its deadline expires Then no success is returned`() = runBlocking {
        // Given
        val fixture = BlockingChannelFixture(timeoutMillis = 30)

        // When
        val result = runCatching { fixture.transfer() }

        // Then
        assertTrue(result.isFailure)
        assertTrue(fixture.closed.await(5, TimeUnit.SECONDS))
    }

    @Test
    fun `Given cancellation before channel open When the channel arrives Then its contents are never consumed`() =
        runBlocking {
            // Given
            val fixture = BlockingChannelFixture()
            fixture.channels.cancel("request")
            var consumed = false

            // When
            val result = runCatching { fixture.channels.use("request", "channel", false) { consumed = true } }

            // Then
            assertTrue(result.isFailure)
            assertFalse(consumed)
            assertTrue(fixture.closed.await(5, TimeUnit.SECONDS))
        }

    private class BlockingChannelFixture(timeoutMillis: Long = 5_000) {
        val started = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val channels = TransferChannels<String>(timeoutMillis) { closed.countDown() }

        suspend fun transfer() = channels.use("request", "channel", closeAfterSuccess = true) {
            started.countDown()
            check(closed.await(5, TimeUnit.SECONDS)) { "Channel was never closed" }
        }
    }
}
