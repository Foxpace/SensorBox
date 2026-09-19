package com.tomasrepcik.sensorbox.phonelaunch

import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import com.tomasrepcik.sensorbox.wearoslib.connection.FakeWearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connection.ObserveWearCapabilityUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.connection.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.connection.WearNode
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommandCodec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhoneLaunchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given connected phone When launch is requested Then versioned command is sent`() = runTest {
        val repository = connectedRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.accept(PhoneLaunchIntent.LaunchRequested)
        advanceUntilIdle()

        assertEquals(PhoneLaunchStatus.SENT, viewModel.state.value.status)
        assertEquals(
            WearCommand.LaunchPhone,
            WearCommandCodec.decode(repository.sentMessages.single().payload).getOrThrow(),
        )
    }

    private fun connectedRepository() = FakeWearConnectionRepository(
        WearConnection.Connected(WearNode("phone", "Phone", isNearby = true)),
    )

    private fun createViewModel(repository: FakeWearConnectionRepository) = PhoneLaunchViewModel(
        ObserveWearCapabilityUseCase(repository),
        SendWearCommandUseCase(SendWearMessageUseCase(repository)),
        AppFailureStore(DiagnosticLogger { }),
    )
}
