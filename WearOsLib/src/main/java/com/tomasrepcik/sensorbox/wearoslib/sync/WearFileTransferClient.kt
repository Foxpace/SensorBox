package com.tomasrepcik.sensorbox.wearoslib.sync

import com.google.android.gms.wearable.ChannelClient
import com.tomasrepcik.sensorbox.core.failure.AppResult
import java.io.InputStream

interface WearFileTransferClient {
    fun cancel(requestId: String)

    suspend fun send(nodeId: String, metadata: WearFileMetadata, input: () -> InputStream): AppResult<Unit>

    suspend fun reject(channel: ChannelClient.Channel): AppResult<Unit>

    suspend fun receive(channel: ChannelClient.Channel, consume: (InputStream) -> AppResult<Unit>): AppResult<Unit>
}
