package com.tomasrepcik.sensorbox.measurements.sync

import androidx.lifecycle.ViewModelStore
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import com.tomasrepcik.sensorbox.wearoslib.connection.FakeWearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connection.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommandCodec
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileMetadata
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchSyncViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val connection = FakeWearConnectionRepository()
    private val storage = WatchSyncStorageFixture()
    private val repo = WatchSyncRepo(com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock())
    private val transfers = SyncTransferFixture()

    @Test
    fun `Given a connected watch When checking Then available measurement counts are shown`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.accept(WatchSyncIntent.CHECK)
        runCurrent()
        val command = sentCommand() as WearCommand.CheckWatchMeasurements
        repo.receive(WearCommand.WatchMeasurementsStatus(command.requestId, 3, 2))
        advanceUntilIdle()

        // Then
        assertEquals(WatchSyncStatus.AVAILABLE, viewModel.state.value.status)
        assertEquals(1, connection.sentMessages.size)
        assertTrue(sentCommand() is WearCommand.CheckWatchMeasurements)
        assertEquals(3, viewModel.state.value.fileCount)
        assertEquals(2, viewModel.state.value.measurementCount)
    }

    @Test
    fun `Given no archive When syncing Then no transfer is requested`() = runTest {
        // Given
        storage.configured = false
        val viewModel = viewModel()

        // When
        viewModel.accept(WatchSyncIntent.COPY)
        advanceUntilIdle()

        // Then
        assertEquals(WatchSyncStatus.FAILED, viewModel.state.value.status)
        assertTrue(connection.sentMessages.isEmpty())
    }

    @Test
    fun `Given an unreachable watch When checking Then a connection error is shown`() = runTest {
        // Given
        connection.failSending(AppError(AppErrorCode.CONNECTIVITY, "Find watch"))
        val viewModel = viewModel()

        // When
        viewModel.accept(WatchSyncIntent.CHECK)
        advanceUntilIdle()

        // Then
        assertEquals(WatchSyncStatus.FAILED, viewModel.state.value.status)
    }

    @Test
    fun `Given an unresponsive watch When the request expires Then retry is possible`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.accept(WatchSyncIntent.CHECK)
        advanceUntilIdle()
        viewModel.accept(WatchSyncIntent.CHECK)
        runCurrent()

        // Then
        assertEquals(2, connection.sentMessages.size)
        assertEquals(WatchSyncStatus.CHECKING, viewModel.state.value.status)
        advanceUntilIdle()
    }

    @Test
    fun `Given an active sync When sync is tapped again Then only one request is sent`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.accept(WatchSyncIntent.COPY)
        runCurrent()

        // When
        viewModel.accept(WatchSyncIntent.COPY)
        val command = sentCommand() as WearCommand.CopyWatchMeasurements
        repo.fileSaved(WearFileMetadata("walk", "sensor.csv", command.requestId))
        repo.receive(WearCommand.WatchMeasurementsStatus(command.requestId, 1, 1, finished = true))
        advanceUntilIdle()

        // Then
        assertEquals(1, connection.sentMessages.size)
        assertEquals(WatchSyncStatus.COMPLETE, viewModel.state.value.status)
    }

    @Test
    fun `Given a pending check When the screen is removed Then sync can be retried`() = runTest {
        // Given
        val viewModel = viewModel()
        val store = ViewModelStore()
        store.put("sync", viewModel)
        viewModel.accept(WatchSyncIntent.CHECK)
        runCurrent()

        // When
        store.clear()

        // Then
        assertEquals(WatchSyncStatus.FAILED, viewModel.state.value.status)
    }

    @Test
    fun `Given failed copy When retry is tapped Then copying is requested again`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.accept(WatchSyncIntent.COPY)
        runCurrent()
        repo.fail("Transfer interrupted")
        runCurrent()

        // When
        viewModel.accept(WatchSyncIntent.RETRY)
        runCurrent()

        // Then
        assertTrue(sentCommand() is WearCommand.CopyWatchMeasurements)
        assertEquals(3, connection.sentMessages.size)
        advanceUntilIdle()
    }

    @Test
    fun `Given failed check When retry is tapped Then no files are requested`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.accept(WatchSyncIntent.CHECK)
        advanceUntilIdle()

        // When
        viewModel.accept(WatchSyncIntent.RETRY)
        runCurrent()

        // Then
        assertTrue(sentCommand() is WearCommand.CheckWatchMeasurements)
        advanceUntilIdle()
    }

    @Test
    fun `Given an unresponsive copy When its deadline expires Then channels close and watch cancellation is sent`() =
        runTest {
            // Given
            val viewModel = viewModel()
            viewModel.accept(WatchSyncIntent.COPY)
            runCurrent()
            val request = sentCommand() as WearCommand.CopyWatchMeasurements

            // When
            advanceUntilIdle()

            // Then
            assertTrue(request.requestId in transfers.cancelled)
            assertEquals(WearCommand.CancelWatchSync(request.requestId), sentCommand())
            assertEquals(WatchSyncStatus.FAILED, viewModel.state.value.status)
            assertEquals(false, repo.syncLock.busy.value)
        }

    private fun sentCommand(): WearCommand = checkNotNull(
        WearCommandCodec.decode(connection.sentMessages.last().payload).getOrNull(),
    )

    private fun viewModel() = WatchSyncViewModel(
        repo,
        SendWearCommandUseCase(SendWearMessageUseCase(connection)),
        storage,
        mainDispatcherRule.dispatcher,
        transfers,
        WatchFileDestination(storage, repo),
    )
}
