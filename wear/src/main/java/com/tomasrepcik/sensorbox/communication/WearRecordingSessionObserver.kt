package com.tomasrepcik.sensorbox.communication

import com.motionapps.sensorservices.session.MeasurementSessionEvent
import com.motionapps.sensorservices.session.MeasurementSessionStore
import com.motionapps.sensorservices.session.MeasurementStopReason
import com.motionapps.wearoslib.protocol.WearStopReason
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.error.toDiagnosticEvent
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
    private val sessionStore: MeasurementSessionStore,
    private val commandHandler: WearCommandHandler,
    private val diagnosticLogger: DiagnosticLogger,
) {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            sessionStore.events.collect { event ->
                if (event is MeasurementSessionEvent.Stopped) onStopped(event)
            }
        }
    }

    private suspend fun onStopped(event: MeasurementSessionEvent.Stopped) {
        event.result.errorOrNull()?.let { error -> diagnosticLogger.record(error.toDiagnosticEvent()) }
        val reason = event.reason.toAutomaticWearReason() ?: return
        commandHandler.onAutomaticStop(reason).onFailure { error ->
            diagnosticLogger.record(error.toDiagnosticEvent())
        }
    }

    private fun MeasurementStopReason.toAutomaticWearReason(): WearStopReason? = when (this) {
        MeasurementStopReason.USER_REQUEST -> null
        MeasurementStopReason.DURATION_EXPIRED -> WearStopReason.DURATION_EXPIRED
        MeasurementStopReason.LOW_BATTERY -> WearStopReason.LOW_BATTERY
        MeasurementStopReason.SOURCE_FAILURE -> WearStopReason.SOURCE_FAILURE
        MeasurementStopReason.SERVICE_DESTROYED -> WearStopReason.SERVICE_DESTROYED
    }
}
