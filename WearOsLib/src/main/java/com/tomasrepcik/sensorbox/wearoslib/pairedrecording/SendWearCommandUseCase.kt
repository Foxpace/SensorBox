package com.tomasrepcik.sensorbox.wearoslib.pairedrecording

import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.suspendFlatMap
import com.tomasrepcik.sensorbox.wearoslib.connection.SendWearMessageUseCase
import javax.inject.Inject

class SendWearCommandUseCase @Inject constructor(private val sendMessage: SendWearMessageUseCase) {
    suspend operator fun invoke(capability: String, path: String, command: WearCommand): AppResult<Unit> =
        WearCommandCodec.encode(command).suspendFlatMap { payload ->
            sendMessage(capability, path, payload)
        }
}
