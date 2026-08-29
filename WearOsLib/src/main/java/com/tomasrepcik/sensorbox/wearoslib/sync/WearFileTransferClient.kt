package com.tomasrepcik.sensorbox.wearoslib.sync

import com.google.android.gms.wearable.ChannelClient
import com.tomasrepcik.sensorbox.core.failure.AppResult
import java.io.InputStream

interface WearFileTransferClient {
    suspend fun send(nodeId: String, metadata: WearFileMetadata, input: () -> InputStream): AppResult<Unit>

    suspend fun receive(channel: ChannelClient.Channel, consume: (InputStream) -> AppResult<Unit>): AppResult<Unit>
}
