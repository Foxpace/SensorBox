package com.tomasrepcik.sensorbox.domain.sync

import com.google.android.gms.wearable.ChannelClient
import com.motionapps.wearoslib.files.WearFilePathCodec
import com.motionapps.wearoslib.files.WearFileTransferClient
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.core.error.suspendFlatMap
import javax.inject.Inject

class ReceiveWearFileUseCase @Inject constructor(
    private val transferClient: WearFileTransferClient,
    private val destination: WearFileDestination,
) {
    suspend operator fun invoke(channel: ChannelClient.Channel): AppResult<Unit> =
        WearFilePathCodec.decode(channel.path).suspendFlatMap { metadata ->
            destination.isReady().flatMap { ready ->
                if (ready) {
                    AppResult.success(Unit)
                } else {
                    AppResult.failure(
                        AppError(AppErrorCode.STORAGE, "Receive Wear file"),
                    )
                }
            }.suspendFlatMap {
                transferClient.receive(channel) { input -> destination.copy(metadata, input) }
            }
        }
}
