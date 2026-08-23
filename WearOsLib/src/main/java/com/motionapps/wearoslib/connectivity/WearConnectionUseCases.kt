package com.motionapps.wearoslib.connectivity

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWearCapabilityUseCase @Inject constructor(private val repository: WearConnectionRepository) {
    operator fun invoke(capability: String): Flow<WearConnection> = repository.observeCapability(capability)
}

class SendWearMessageUseCase @Inject constructor(private val repository: WearConnectionRepository) {
    suspend operator fun invoke(capability: String, path: String, message: String): Result<Unit> =
        invoke(capability, path, message.encodeToByteArray())

    suspend operator fun invoke(capability: String, path: String, payload: ByteArray): Result<Unit> =
        repository.sendMessage(capability, path, payload)
}
