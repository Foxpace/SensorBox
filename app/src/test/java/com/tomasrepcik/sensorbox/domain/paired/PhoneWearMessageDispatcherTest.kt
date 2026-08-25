package com.tomasrepcik.sensorbox.domain.paired

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommandCodec
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneWearMessageDispatcherTest {
    @Test
    fun `Given encoded command bytes When dispatched Then no Google callback type is required`() = runTest {
        val commands = mutableListOf<WearCommand>()
        val dispatcher = PhoneWearMessageDispatcher(
            commandHandler = PhoneWearCommandPolicy { command ->
                commands += command
                AppResult.success(Unit)
            },
            diagnosticLogger = DiagnosticLogger { },
        )
        val payload = WearCommandCodec.encode(WearCommand.LaunchPhone).getOrThrow()

        val result = dispatcher.dispatch(PHONE_MESSAGE_PATH, payload)

        assertTrue(result.isSuccess)
        assertEquals(listOf(WearCommand.LaunchPhone), commands)
    }

    @Test
    fun `Given a protocol v1 payload When dispatched Then command policy is not called`() = runTest {
        var calls = 0
        val dispatcher = PhoneWearMessageDispatcher(
            commandHandler = PhoneWearCommandPolicy {
                calls += 1
                AppResult.success(Unit)
            },
            diagnosticLogger = DiagnosticLogger { },
        )

        val result = dispatcher.dispatch(
            PHONE_MESSAGE_PATH,
            byteArrayOf(0x53, 0x42, 0x58, 0x31, 1, 1),
        )

        assertTrue(result.isFailure)
        assertEquals(0, calls)
    }
}
