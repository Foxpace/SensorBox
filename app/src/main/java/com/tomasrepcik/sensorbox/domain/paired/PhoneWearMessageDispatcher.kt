package com.tomasrepcik.sensorbox.domain.paired

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.suspendFlatMap
import com.tomasrepcik.sensorbox.core.error.toDiagnosticEvent
import com.tomasrepcik.sensorbox.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommandCodec
import javax.inject.Inject

class PhoneWearMessageDispatcher @Inject constructor(
    private val commandHandler: PhoneWearCommandHandler,
    private val diagnosticLogger: DiagnosticLogger,
) {
    suspend fun dispatch(path: String, payload: ByteArray): AppResult<Unit> {
        if (path != PHONE_MESSAGE_PATH) return AppResult.success(Unit)
        return WearCommandCodec.decode(payload)
            .suspendFlatMap(commandHandler::handle)
            .onFailure { error -> diagnosticLogger.record(error.toDiagnosticEvent()) }
    }
}
