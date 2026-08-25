package com.tomasrepcik.sensorbox.communication

import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearSessionCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearAcknowledgementInbox @Inject constructor() {
    private val acknowledgements = ConcurrentHashMap<Key, WearCommand.Acknowledgement>()
    private val updates = MutableSharedFlow<WearCommand.Acknowledgement>(extraBufferCapacity = BUFFER_SIZE)

    fun publish(acknowledgement: WearCommand.Acknowledgement) {
        acknowledgements[acknowledgement.key()] = acknowledgement
        updates.tryEmit(acknowledgement)
    }

    fun clear(sessionId: String, command: WearSessionCommand) {
        acknowledgements.remove(Key(sessionId, command))
    }

    suspend fun await(sessionId: String, command: WearSessionCommand): WearCommand.Acknowledgement {
        val key = Key(sessionId, command)
        acknowledgements[key]?.let { return it }
        return updates.first { acknowledgement -> acknowledgement.key() == key }
    }

    private fun WearCommand.Acknowledgement.key() = Key(sessionId, command)

    private data class Key(val sessionId: String, val command: WearSessionCommand)

    private companion object {
        const val BUFFER_SIZE = 32
    }
}
