package com.tomasrepcik.sensorbox.measurements.sync

import com.google.android.gms.wearable.ChannelClient
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileMetadata
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileTransferClient
import java.io.InputStream

internal class SyncTransferFixture : WearFileTransferClient {
    val cancelled = mutableListOf<String>()
    override fun cancel(requestId: String) {
        cancelled += requestId
    }
    override suspend fun send(nodeId: String, metadata: WearFileMetadata, input: () -> InputStream): AppResult<Unit> =
        error("Not used")
    override suspend fun reject(channel: ChannelClient.Channel): AppResult<Unit> = AppResult.success(Unit)
    override suspend fun receive(
        channel: ChannelClient.Channel,
        consume: (InputStream) -> AppResult<Unit>,
    ): AppResult<Unit> = error("Not used")
}
