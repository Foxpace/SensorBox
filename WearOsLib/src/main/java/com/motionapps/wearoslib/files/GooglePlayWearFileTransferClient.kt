package com.motionapps.wearoslib.files

import android.content.Context
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.suspendAppResult
import com.tomasrepcik.sensorbox.core.error.suspendFlatMap
import com.tomasrepcik.sensorbox.core.error.withAppError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GooglePlayWearFileTransferClient @Inject constructor(@ApplicationContext context: Context) :
    WearFileTransferClient {
    private val channelClient = Wearable.getChannelClient(context)

    override suspend fun send(
        nodeId: String,
        metadata: WearFileMetadata,
        input: () -> java.io.InputStream,
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        WearFilePathCodec.encode(metadata).suspendFlatMap { path ->
            suspendAppResult(AppErrorCode.CONNECTIVITY, "Open Wear channel") {
                channelClient.openChannel(nodeId, path).await()
            }
        }.suspendFlatMap { channel ->
            val transfer = suspendAppResult(AppErrorCode.CONNECTIVITY, "Write Wear channel") {
                input().use { source ->
                    channelClient.getOutputStream(channel).await().use(source::copyTo)
                }
            }
            val close = suspendAppResult(AppErrorCode.CONNECTIVITY, "Close Wear channel") {
                channelClient.close(channel).await()
            }
            listOf(transfer, close).combineAppResults(AppErrorCode.CONNECTIVITY, "Send Wear file")
        }.withAppError(AppErrorCode.CONNECTIVITY, "Send Wear file")
    }

    override suspend fun receive(
        channel: ChannelClient.Channel,
        consume: (java.io.InputStream) -> AppResult<Unit>,
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        val transfer = suspendAppResult(AppErrorCode.CONNECTIVITY, "Open Wear input stream") {
            channelClient.getInputStream(channel).await()
        }.suspendFlatMap { input ->
            input.use(consume)
        }
        val close = suspendAppResult(AppErrorCode.CONNECTIVITY, "Close Wear channel") {
            channelClient.close(channel).await()
        }
        listOf(transfer, close).combineAppResults(AppErrorCode.CONNECTIVITY, "Receive Wear file")
    }
}
