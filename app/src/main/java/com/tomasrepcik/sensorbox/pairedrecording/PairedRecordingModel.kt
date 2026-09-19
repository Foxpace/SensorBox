package com.tomasrepcik.sensorbox.pairedrecording

import com.tomasrepcik.sensorbox.recording.setup.RecordingSetup
import com.tomasrepcik.sensorbox.recording.setup.StartedPhoneRecording
import com.tomasrepcik.sensorbox.recording.setup.toWatchRecordingRequest
import com.tomasrepcik.sensorbox.wearoslib.pairedrecording.WearCommand

internal sealed interface PairedRecordingState {
    data object Idle : PairedRecordingState

    data class Starting(val sessionId: String, val includesWatch: Boolean) : PairedRecordingState

    data class Recording(val sessionId: String, val includesWatch: Boolean) : PairedRecordingState

    data class Stopping(val sessionId: String, val includesWatch: Boolean) : PairedRecordingState
}

internal data class ActivePairedRecording(val sessionId: String, val includesWatch: Boolean)

internal fun PairedRecordingState.activeRecording(): ActivePairedRecording? = when (this) {
    is PairedRecordingState.Starting -> ActivePairedRecording(sessionId, includesWatch)
    is PairedRecordingState.Recording -> ActivePairedRecording(sessionId, includesWatch)
    is PairedRecordingState.Stopping -> ActivePairedRecording(sessionId, includesWatch)
    PairedRecordingState.Idle -> null
}

internal fun RecordingSetup.includesWatchRecording(): Boolean = watchSensorIds.isNotEmpty() || watchIncludesGps

internal fun StartedPhoneRecording.toWatchStartCommand(request: RecordingSetup) = WearCommand.StartRecording(
    sessionId = sessionId,
    request = toWatchRecordingRequest(request),
)
