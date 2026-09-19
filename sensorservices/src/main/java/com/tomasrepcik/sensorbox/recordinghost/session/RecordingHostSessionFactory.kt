package com.tomasrepcik.sensorbox.recordinghost.session

import android.app.Service
import android.os.SystemClock
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingRequest
import com.tomasrepcik.sensorbox.recordinghost.session.ServiceController
import com.tomasrepcik.sensorbox.recordinghost.storage.StorageHandler
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

internal class RecordingHostSessionFactory @Inject constructor(
    private val storage: StorageHandler,
    private val diagnosticLogger: DiagnosticLogger,
    private val clock: EpochClock,
    private val sessionStore: RecordingSessionStore,
) {
    fun create(service: Service, request: RecordingRequest, scope: CoroutineScope): RecordingHostSession {
        val execution = ServiceController(
            context = service,
            request = request,
            scope = scope,
            storage = storage,
            diagnosticLogger = diagnosticLogger,
            clock = clock,
        )
        lateinit var session: RecordingHostSession
        val environment = RecordingHostResources(
            service = service,
            scope = scope,
            requestStop = { reason -> session.requestStop(reason) },
            playAlarm = { execution.playAlarm() },
        )
        session = RecordingHostSession(
            request = request,
            execution = execution,
            environment = environment,
            sessionStore = sessionStore,
            diagnosticLogger = diagnosticLogger,
            scope = scope,
            elapsedRealtimeMillis = SystemClock::elapsedRealtime,
        )
        return session
    }
}
