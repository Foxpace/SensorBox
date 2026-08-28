package com.tomasrepcik.sensorbox.domain.paired

import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingOperation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchRecordingResultInbox @Inject constructor() {
    private val results = ConcurrentHashMap<Key, WearCommand.RecordingResult>()
    private val updates = MutableSharedFlow<WearCommand.RecordingResult>(extraBufferCapacity = BUFFER_SIZE)

    fun publish(result: WearCommand.RecordingResult) {
        results[result.key()] = result
        updates.tryEmit(result)
    }

    fun clear(sessionId: String, operation: WearRecordingOperation) {
        results.remove(Key(sessionId, operation))
    }

    suspend fun await(sessionId: String, operation: WearRecordingOperation): WearCommand.RecordingResult {
        val key = Key(sessionId, operation)
        results[key]?.let { return it }
        return updates.first { result -> result.key() == key }
    }

    private fun WearCommand.RecordingResult.key() = Key(sessionId, operation)

    private data class Key(val sessionId: String, val operation: WearRecordingOperation)

    private companion object {
        const val BUFFER_SIZE = 16
    }
}
