package com.tomasrepcik.sensorbox.domain.paired

import com.motionapps.wearoslib.WearOsConstants.PHONE_MESSAGE_PATH
import com.motionapps.wearoslib.protocol.WearCommandCodec
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.suspendFlatMap
import com.tomasrepcik.sensorbox.core.error.toDiagnosticEvent
import javax.inject.Inject

class PhoneWearMessageDispatcher @Inject constructor(
    private val commandHandler: PhoneWearCommandPolicy,
    private val diagnosticLogger: DiagnosticLogger,
) {
    suspend fun dispatch(path: String, payload: ByteArray): AppResult<Unit> {
        if (path != PHONE_MESSAGE_PATH) return AppResult.success(Unit)
        return WearCommandCodec.decode(payload)
            .suspendFlatMap(commandHandler::handle)
            .onFailure { error -> diagnosticLogger.record(error.toDiagnosticEvent()) }
    }
}
