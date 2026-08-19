package com.motionapps.wearoslib.protocol

import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.suspendFlatMap
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import javax.inject.Inject

class SendWearCommandUseCase @Inject constructor(private val sendMessage: SendWearMessageUseCase) {
    suspend operator fun invoke(capability: String, path: String, command: WearCommand): AppResult<Unit> =
        WearCommandCodec.encode(command).suspendFlatMap { payload ->
            sendMessage(capability, path, payload)
        }
}
