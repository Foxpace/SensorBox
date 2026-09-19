package com.tomasrepcik.sensorbox.wearoslib.connection

import com.tomasrepcik.sensorbox.core.failure.AppResult
import kotlinx.coroutines.flow.Flow

interface WearConnectionRepository {
    fun observeCapability(capability: String): Flow<WearConnection>

    suspend fun findNode(capability: String): WearNode?

    suspend fun sendMessage(capability: String, path: String, payload: ByteArray): AppResult<Unit>
}
