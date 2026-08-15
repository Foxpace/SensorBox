package com.motionapps.wearoslib.files

import com.google.android.gms.wearable.ChannelClient
import java.io.InputStream

interface WearFileTransferClient {
    suspend fun send(nodeId: String, metadata: WearFileMetadata, input: () -> InputStream): Result<Unit>

    suspend fun receive(channel: ChannelClient.Channel, consume: (InputStream) -> Result<Unit>): Result<Unit>
}
