package com.tomasrepcik.sensorbox.domain.sync

import com.google.android.gms.wearable.ChannelClient
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.core.error.suspendFlatMap
import com.tomasrepcik.sensorbox.wearoslib.files.WearFilePathCodec
import com.tomasrepcik.sensorbox.wearoslib.files.WearFileTransferClient
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
