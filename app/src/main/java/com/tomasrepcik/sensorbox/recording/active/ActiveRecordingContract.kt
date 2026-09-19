package com.tomasrepcik.sensorbox.recording.active

import com.tomasrepcik.sensorbox.recording.RecordingScreenEffect
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState

sealed interface ActiveRecordingIntent {
    data object StopRecording : ActiveRecordingIntent
    data class AddAnnotation(val text: String) : ActiveRecordingIntent
}

sealed interface ActiveRecordingEffect : RecordingScreenEffect {
    data object RecordingStopped : ActiveRecordingEffect
}

object RecordingSessionStateReducer {
    fun sessionChanged(state: RecordingState, session: RecordingSessionState): RecordingState = state.copy(
        session = session,
        elapsedSeconds = 0,
        isStarting = if (session is RecordingSessionState.Running) false else state.isStarting,
        startCountdownSeconds = if (session is RecordingSessionState.Running) null else state.startCountdownSeconds,
    )
}
