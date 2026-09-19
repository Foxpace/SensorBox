package com.tomasrepcik.sensorbox.wearoslib.connection

import com.tomasrepcik.sensorbox.core.failure.AppResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWearCapabilityUseCase @Inject constructor(private val repository: WearConnectionRepository) {
    operator fun invoke(capability: String): Flow<WearConnection> = repository.observeCapability(capability)
}

class SendWearMessageUseCase @Inject constructor(private val repository: WearConnectionRepository) {
    suspend operator fun invoke(capability: String, path: String, payload: ByteArray): AppResult<Unit> =
        repository.sendMessage(capability, path, payload)
}
