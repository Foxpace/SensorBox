package com.tomasrepcik.sensorbox.wearoslib.sync

import android.content.Context
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.suspendAppResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class GooglePlayWearFileTransferClient @Inject constructor(@ApplicationContext context: Context) :
    WearFileTransferClient {
    private val channelClient = Wearable.getChannelClient(context)
    private val requests = TransferChannels<ChannelClient.Channel> { channelClient.close(it) }

    override fun cancel(requestId: String) = requests.cancel(requestId)

    override suspend fun send(nodeId: String, metadata: WearFileMetadata, input: () -> InputStream): AppResult<Unit> =
        withContext(Dispatchers.IO) {
            suspendAppResult(AppErrorCode.CONNECTIVITY, "Send watch measurement") {
                val path = WearFilePathCodec.encode(metadata).getOrThrow()
                val opening = channelClient.openChannel(nodeId, path)
                var opened: ChannelClient.Channel? = null
                try {
                    opened = withTimeoutOrNull(15_000L.milliseconds) { opening.await() }
                } finally {
                    if (opened == null) opening.addOnSuccessListener { channelClient.close(it) }
                }
                val channel = checkNotNull(opened) { "Opening watch transfer timed out" }
                requests.use(metadata.requestId, channel, closeAfterSuccess = true) {
                    // Open the acknowledgement stream before the peer can finish saving.
                    channelClient.getInputStream(channel).await().use { acknowledgement ->
                        input().use { source ->
                            channelClient.getOutputStream(channel).await().use { output -> source.copyTo(output) }
                        }
                        check(acknowledgement.read() == FILE_SAVED) { "Phone did not confirm saving the measurement" }
                    }
                }
            }
        }

    override suspend fun reject(channel: ChannelClient.Channel): AppResult<Unit> =
        suspendAppResult(AppErrorCode.CONNECTIVITY, "Reject watch transfer") {
            channelClient.close(channel)
        }

    override suspend fun receive(
        channel: ChannelClient.Channel,
        consume: (InputStream) -> AppResult<Unit>,
    ): AppResult<Unit> = withContext(Dispatchers.IO) {
        val metadata = WearFilePathCodec.decode(channel.path).getOrNull()
            ?: return@withContext AppResult.failure(AppError(AppErrorCode.VALIDATION, "Decode watch transfer"))
        suspendAppResult(AppErrorCode.CONNECTIVITY, "Receive watch measurement") {
            requests.use(metadata.requestId, channel, closeAfterSuccess = false) {
                channelClient.getInputStream(channel).await().use { consume(it).getOrThrow() }
                channelClient.getOutputStream(channel).await().use { output ->
                    output.write(FILE_SAVED)
                    output.flush()
                }
                // The sender closes the channel after reading the acknowledgement.
            }
        }
    }

    private companion object {
        const val FILE_SAVED = 1
    }
}
