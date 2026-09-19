package com.tomasrepcik.sensorbox.measurements.sync

import com.google.android.gms.wearable.ChannelClient
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFilePathCodec
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileTransferClient
import javax.inject.Inject

class ReceiveWatchFileUseCase @Inject constructor(
    private val transferClient: WearFileTransferClient,
    private val destination: WatchFileDestination,
    private val watchSync: WatchSyncRepo,
) {
    suspend operator fun invoke(channel: ChannelClient.Channel): AppResult<Unit> {
        val metadata = WearFilePathCodec.decode(channel.path).getOrNull()
        if (metadata == null || !watchSync.accepts(metadata.requestId)) {
            transferClient.reject(channel)
            return AppResult.failure(AppError(AppErrorCode.CONFLICT, "Reject expired watch transfer"))
        }
        val result = transferClient.receive(channel) { input -> destination.copy(metadata, input) }
        if (result.isSuccess) {
            watchSync.fileSaved(metadata)
        } else {
            watchSync.fail(
                "Could not save a watch measurement. Check the recording folder and retry.",
                metadata.requestId,
            )
            destination.discard(metadata.requestId)
        }
        return result
    }
}
