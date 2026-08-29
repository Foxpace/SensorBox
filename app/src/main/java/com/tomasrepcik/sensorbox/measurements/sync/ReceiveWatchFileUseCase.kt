package com.tomasrepcik.sensorbox.measurements.sync

import com.google.android.gms.wearable.ChannelClient
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.flatMap
import com.tomasrepcik.sensorbox.core.failure.suspendFlatMap
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFilePathCodec
import com.tomasrepcik.sensorbox.wearoslib.sync.WearFileTransferClient
import javax.inject.Inject

class ReceiveWatchFileUseCase @Inject constructor(
    private val transferClient: WearFileTransferClient,
    private val destination: WatchFileDestination,
) {
    suspend operator fun invoke(channel: ChannelClient.Channel): AppResult<Unit> =
        WearFilePathCodec.decode(channel.path).suspendFlatMap { metadata ->
            destination.isReady().flatMap { ready ->
                if (ready) {
                    AppResult.success(Unit)
                } else {
                    AppResult.failure(
                        AppError(AppErrorCode.STORAGE, "Receive watch file"),
                    )
                }
            }.suspendFlatMap {
                transferClient.receive(channel) { input -> destination.copy(metadata, input) }
            }
        }
}
