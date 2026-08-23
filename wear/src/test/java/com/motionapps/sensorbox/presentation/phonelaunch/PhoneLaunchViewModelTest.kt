package com.motionapps.sensorbox.presentation.phonelaunch

import com.motionapps.sensorbox.testing.MainDispatcherRule
import com.motionapps.wearoslib.connectivity.FakeWearConnectionRepository
import com.motionapps.wearoslib.connectivity.ObserveWearCapabilityUseCase
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import com.motionapps.wearoslib.connectivity.WearConnection
import com.motionapps.wearoslib.connectivity.WearNode
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
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
        SendWearMessageUseCase(repository),
    )
}
