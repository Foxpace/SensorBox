package com.tomasrepcik.sensorbox.communication

import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.toDiagnosticEvent
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStopReason
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStopped
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearStopReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearRecordingSessionObserver @Inject constructor(
    private val sessionStore: RecordingSessionStore,
    private val commandHandler: WearCommandHandler,
    private val diagnosticLogger: DiagnosticLogger,
) {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            sessionStore.events.collect(::onStopped)
        }
    }

    private suspend fun onStopped(event: RecordingSessionStopped) {
        event.result.errorOrNull()?.let { error -> diagnosticLogger.record(error.toDiagnosticEvent()) }
        val reason = event.reason.toAutomaticWatchReason() ?: return
        commandHandler.onAutomaticStop(reason).onFailure { error ->
            diagnosticLogger.record(error.toDiagnosticEvent())
        }
    }

    private fun RecordingSessionStopReason.toAutomaticWatchReason(): WearStopReason? = when (this) {
        RecordingSessionStopReason.USER_REQUEST -> null
        RecordingSessionStopReason.DURATION_EXPIRED -> WearStopReason.DURATION_EXPIRED
        RecordingSessionStopReason.LOW_BATTERY -> WearStopReason.LOW_BATTERY
        RecordingSessionStopReason.SOURCE_FAILURE -> WearStopReason.SOURCE_FAILURE
        RecordingSessionStopReason.SERVICE_DESTROYED -> WearStopReason.SERVICE_DESTROYED
    }
}
