package com.tomasrepcik.sensorbox.wearoslib.files

import android.content.Context
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.combineAppResults
import com.tomasrepcik.sensorbox.core.error.suspendAppResult
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
        val path = when (val encoded = WearFilePathCodec.encode(metadata)) {
            is AppResult.Failure -> return@withContext encoded
            is AppResult.Success -> encoded.value
        }
        val channel = when (val opened = openChannel(nodeId, path)) {
            is AppResult.Failure -> return@withContext opened
            is AppResult.Success -> opened.value
        }
        val transfer = writeChannel(channel, input)
        val close = closeChannel(channel)
        listOf(transfer, close)
            .combineAppResults(AppErrorCode.CONNECTIVITY, "Send Wear file")
            .withAppError(AppErrorCode.CONNECTIVITY, "Send Wear file")
    }

    override suspend fun receive(
        channel: ChannelClient.Channel,
        consume: (java.io.InputStream) -> AppResult<Unit>,
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        val opened = suspendAppResult(AppErrorCode.CONNECTIVITY, "Open Wear input stream") {
            channelClient.getInputStream(channel).await()
        }
        val transfer = when (opened) {
            is AppResult.Failure -> opened
            is AppResult.Success -> opened.value.use(consume)
        }
        val close = closeChannel(channel)
        listOf(transfer, close).combineAppResults(AppErrorCode.CONNECTIVITY, "Receive watch file")
    }

    private suspend fun openChannel(nodeId: String, path: String): AppResult<ChannelClient.Channel> =
        suspendAppResult(AppErrorCode.CONNECTIVITY, "Open Wear channel") {
            channelClient.openChannel(nodeId, path).await()
        }

    private suspend fun writeChannel(
        channel: ChannelClient.Channel,
        input: () -> java.io.InputStream,
    ): AppResult<Unit> = suspendAppResult(AppErrorCode.CONNECTIVITY, "Write Wear channel") {
        input().use { source ->
            channelClient.getOutputStream(channel).await().use(source::copyTo)
        }
    }

    private suspend fun closeChannel(channel: ChannelClient.Channel): AppResult<Unit> =
        suspendAppResult(AppErrorCode.CONNECTIVITY, "Close Wear channel") {
            channelClient.close(channel).await()
        }
}
