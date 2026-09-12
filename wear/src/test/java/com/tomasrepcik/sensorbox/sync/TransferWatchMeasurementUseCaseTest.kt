package com.tomasrepcik.sensorbox.sync

import com.google.android.gms.wearable.ChannelClient
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileMetadata
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileTransferClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.InputStream

class TransferWatchMeasurementUseCaseTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `Given saved files When every acknowledgement arrives Then the watch measurement is deleted`() = runTest {
        // Given
        val fixture = fixture()

        // When
        val result = fixture.transfer("phone", fixture.files, "request")

        // Then
        assertTrue(result.isSuccess)
        assertEquals(listOf("samples", "metadata"), fixture.client.received.take(2))
        assertEquals(3, fixture.client.received.size)
        assertFalse(fixture.folder.exists())
    }

    @Test
    fun `Given a failed file When transfer ends Then all watch originals remain`() = runTest {
        // Given
        val fixture = fixture()
        fixture.client.failAt = 2

        // When
        val result = fixture.transfer("phone", fixture.files, "request")

        // Then
        assertTrue(result.isFailure)
        assertTrue(fixture.files.all(File::exists))
    }

    @Test
    fun `Given a changing measurement When transfer finishes Then watch originals remain`() = runTest {
        // Given
        val fixture = fixture()
        fixture.client.afterSend = { fixture.files.first().writeText("changed") }

        // When
        val result = fixture.transfer("phone", fixture.files, "request")

        // Then
        assertTrue(result.isFailure)
        assertTrue(fixture.files.all(File::exists))
    }

    @Test
    fun `Given recording is stopping When sync starts Then files are neither sent nor deleted`() = runTest {
        // Given
        val fixture = fixture()
        fixture.sessions.markStopping()

        // When
        val result = fixture.transfer("phone", fixture.files, "request")

        // Then
        assertTrue(result.isFailure)
        assertTrue(fixture.client.received.isEmpty())
        assertTrue(fixture.files.all(File::exists))
    }

    @Test
    fun `Given staged files When the folder acknowledgement is lost Then watch originals remain`() = runTest {
        // Given
        val fixture = fixture()
        fixture.client.failAt = 3

        // When
        val result = fixture.transfer("phone", fixture.files, "request")

        // Then
        assertTrue(result.isFailure)
        assertTrue(fixture.files.all(File::exists))
        assertEquals(3, fixture.client.received.size)
    }

    @Test
    fun `Given interrupted cleanup When retried Then the phone copy is not replaced`() = runTest {
        // Given
        val fixture = fixture()
        WatchMeasurementCleanup().confirm(fixture.folder, fixture.files)
        fixture.files.first().delete()

        // When
        val result = fixture.transfer("phone", fixture.files.drop(1), "retry")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(fixture.client.received.isEmpty())
        assertFalse(fixture.folder.exists())
    }

    private fun fixture(): Fixture {
        val folder = temporary.newFolder("measurement")
        val files = listOf(
            File(folder, "sensor.csv").apply { writeText("samples") },
            File(folder, "metadata.json").apply { writeText("metadata") },
        )
        val client = SavingPhone()
        val sessions = RecordingSessionStore()
        return Fixture(folder, files, client, sessions, TransferWatchMeasurementUseCase(client, sessions))
    }

    private data class Fixture(
        val folder: File,
        val files: List<File>,
        val client: SavingPhone,
        val sessions: RecordingSessionStore,
        val transfer: TransferWatchMeasurementUseCase,
    )

    private class SavingPhone : WearFileTransferClient {
        override fun cancel(requestId: String) = Unit

        val received = mutableListOf<String>()
        var failAt = 0
        var afterSend: () -> Unit = {}

        override suspend fun send(
            nodeId: String,
            metadata: WearFileMetadata,
            input: () -> InputStream,
        ): AppResult<Unit> {
            received += input().bufferedReader().use { it.readText() }
            afterSend()
            return if (received.size == failAt) {
                AppResult.failure(AppError(AppErrorCode.CONNECTIVITY, "Save file on phone"))
            } else {
                AppResult.success(Unit)
            }
        }

        override suspend fun reject(channel: ChannelClient.Channel): AppResult<Unit> = error("Unused")

        override suspend fun receive(
            channel: ChannelClient.Channel,
            consume: (InputStream) -> AppResult<Unit>,
        ): AppResult<Unit> = error("Unused")
    }
}
