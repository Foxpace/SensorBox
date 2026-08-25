package com.motionapps.wearoslib.connectivity

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.AppError

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWearConnectionRepository(
    initialConnection: WearConnection = WearConnection.Disconnected,
) : WearConnectionRepository {
    private val connection = MutableStateFlow(initialConnection)
    private var sendResult: AppResult<Unit> = AppResult.success(Unit)

    val sentMessages = mutableListOf<SentWearMessage>()

    override fun observeCapability(capability: String): Flow<WearConnection> = connection

    override suspend fun findNode(capability: String): WearNode? =
        (connection.value as? WearConnection.Connected)?.node

    override suspend fun sendMessage(
        capability: String,
        path: String,
        payload: ByteArray,
    ): AppResult<Unit> {
        sentMessages += SentWearMessage(capability, path, payload.copyOf())
        return sendResult
    }

    fun emit(newConnection: WearConnection) {
        connection.value = newConnection
    }

    fun failSending(error: AppError) {
        sendResult = AppResult.failure(error)
    }
}

data class SentWearMessage(
    val capability: String,
    val path: String,
    val payload: ByteArray,
)
