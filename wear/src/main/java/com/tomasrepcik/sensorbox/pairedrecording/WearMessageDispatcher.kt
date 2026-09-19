package com.tomasrepcik.sensorbox.pairedrecording

import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.failure.suspendFlatMap
import com.tomasrepcik.sensorbox.core.failure.toDiagnosticEvent
import com.tomasrepcik.sensorbox.wearoslib.connection.WearOsConstants.WEAR_MESSAGE_PATH
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommandCodec
import javax.inject.Inject

class WearMessageDispatcher @Inject constructor(
    private val commandHandler: WearCommandHandler,
    private val diagnosticLogger: DiagnosticLogger,
) {
    suspend fun dispatch(path: String, payload: ByteArray): AppResult<Unit> {
        if (path != WEAR_MESSAGE_PATH) return AppResult.success(Unit)
        return WearCommandCodec.decode(payload)
            .suspendFlatMap(commandHandler::handle)
            .onFailure { error -> diagnosticLogger.record(error.toDiagnosticEvent()) }
    }
}
