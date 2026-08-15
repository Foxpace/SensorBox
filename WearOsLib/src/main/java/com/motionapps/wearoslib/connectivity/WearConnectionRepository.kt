package com.motionapps.wearoslib.connectivity

import kotlinx.coroutines.flow.Flow

interface WearConnectionRepository {
    fun observeCapability(capability: String): Flow<WearConnection>

    suspend fun findNode(capability: String): WearNode?

    suspend fun sendMessage(capability: String, path: String, payload: ByteArray): Result<Unit>
}
