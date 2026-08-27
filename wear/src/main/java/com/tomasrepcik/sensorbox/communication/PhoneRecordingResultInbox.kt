package com.tomasrepcik.sensorbox.communication

import com.tomasrepcik.sensorbox.wearoslib.protocol.WearCommand
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearRecordingAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneRecordingResultInbox @Inject constructor() {
    private val results = ConcurrentHashMap<Key, WearCommand.RecordingResult>()
    private val updates = MutableSharedFlow<WearCommand.RecordingResult>(extraBufferCapacity = BUFFER_SIZE)

    fun publish(result: WearCommand.RecordingResult) {
        results[result.key()] = result
        updates.tryEmit(result)
    }

    fun clear(sessionId: String, action: WearRecordingAction) {
        results.remove(Key(sessionId, action))
    }

    suspend fun await(sessionId: String, action: WearRecordingAction): WearCommand.RecordingResult {
        val key = Key(sessionId, action)
        results[key]?.let { return it }
        return updates.first { result -> result.key() == key }
    }

    private fun WearCommand.RecordingResult.key() = Key(sessionId, action)

    private data class Key(val sessionId: String, val action: WearRecordingAction)

    private companion object {
        const val BUFFER_SIZE = 16
    }
}
