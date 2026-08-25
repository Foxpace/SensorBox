package com.motionapps.wearoslib.protocol

import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.suspendFlatMap
import javax.inject.Inject

class SendWearCommandUseCase @Inject constructor(private val sendMessage: SendWearMessageUseCase) {
    suspend operator fun invoke(capability: String, path: String, command: WearCommand): AppResult<Unit> =
        WearCommandCodec.encode(command).suspendFlatMap { payload ->
            sendMessage(capability, path, payload)
        }
}
