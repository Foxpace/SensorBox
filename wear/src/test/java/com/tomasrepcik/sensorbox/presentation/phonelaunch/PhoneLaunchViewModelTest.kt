package com.tomasrepcik.sensorbox.presentation.phonelaunch

import com.tomasrepcik.sensorbox.core.error.AppFailureStore
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import com.tomasrepcik.sensorbox.wearoslib.connectivity.FakeWearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connectivity.ObserveWearCapabilityUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearConnection
import com.tomasrepcik.sensorbox.wearoslib.connectivity.WearNode
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommandCodec
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
