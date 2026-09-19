package com.tomasrepcik.sensorbox.recording.setup

import com.tomasrepcik.sensorbox.recording.RecordingDraft
import com.tomasrepcik.sensorbox.recording.RecordingMessage
import com.tomasrepcik.sensorbox.recording.RecordingScreenEffect
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.withDraft

sealed interface RecordingSetupIntent {
    data class LoadDraft(val draft: RecordingDraft) : RecordingSetupIntent
    data object ChooseRecordingArchive : RecordingSetupIntent
    data object StartRecording : RecordingSetupIntent
    data object ClearMessage : RecordingSetupIntent
    data class SetCustomMeasurementName(val value: String) : RecordingSetupIntent
    data class SetStartDelay(val seconds: Int) : RecordingSetupIntent
    data class SetDuration(val seconds: Int) : RecordingSetupIntent
    data class SetNotes(val value: String) : RecordingSetupIntent
    data class SetAlarmOffsets(val value: String) : RecordingSetupIntent
    data class SetActivityRecognition(val enabled: Boolean) : RecordingSetupIntent
    data class SetActivityRecognitionPeriod(val seconds: Int) : RecordingSetupIntent
    data class SetSignificantMotionRecording(val enabled: Boolean) : RecordingSetupIntent
    data class SetSamplingPeriod(val index: Int) : RecordingSetupIntent
    data class SetStopOnLowBattery(val enabled: Boolean) : RecordingSetupIntent
    data class SetWakeLock(val enabled: Boolean) : RecordingSetupIntent
    data class SetKeepScreenAwake(val enabled: Boolean) : RecordingSetupIntent
    data class SetGpsInterval(val seconds: Int) : RecordingSetupIntent
    data class SetGpsDistance(val meters: Int) : RecordingSetupIntent
}

sealed interface RecordingSetupEffect : RecordingScreenEffect {
    data object PickRecordingArchive : RecordingSetupEffect
    data class RequestPermissions(val permissions: Set<String>) : RecordingSetupEffect
}

object RecordingSetupReducer {
    fun reduce(state: RecordingState, intent: RecordingSetupIntent): RecordingState = when (intent) {
        is RecordingSetupIntent.LoadDraft -> state.withDraft(intent.draft)

        RecordingSetupIntent.ClearMessage -> state.copy(message = RecordingMessage.NONE, errorCode = null)

        is RecordingSetupIntent.SetCustomMeasurementName -> state.copy(customMeasurementName = intent.value)

        is RecordingSetupIntent.SetStartDelay -> state.copy(startDelaySeconds = intent.seconds.coerceAtLeast(0))

        is RecordingSetupIntent.SetDuration -> state.copy(durationSeconds = intent.seconds.coerceAtLeast(0))

        is RecordingSetupIntent.SetNotes -> state.copy(notes = intent.value)

        is RecordingSetupIntent.SetAlarmOffsets -> state.copy(alarmOffsets = intent.value)

        is RecordingSetupIntent.SetActivityRecognition -> state.copy(activityRecognition = intent.enabled)

        is RecordingSetupIntent.SetActivityRecognitionPeriod -> state.copy(
            activityRecognitionPeriodSeconds = intent.seconds.coerceAtLeast(1),
        )

        is RecordingSetupIntent.SetSignificantMotionRecording -> state.copy(significantMotion = intent.enabled)

        RecordingSetupIntent.ChooseRecordingArchive,
        RecordingSetupIntent.StartRecording,
        is RecordingSetupIntent.SetSamplingPeriod,
        is RecordingSetupIntent.SetStopOnLowBattery,
        is RecordingSetupIntent.SetWakeLock,
        is RecordingSetupIntent.SetKeepScreenAwake,
        is RecordingSetupIntent.SetGpsInterval,
        is RecordingSetupIntent.SetGpsDistance,
        -> state
    }

    fun recordingStartRequested(state: RecordingState, countdownSeconds: Int): RecordingState = state.copy(
        isStarting = true,
        startCountdownSeconds = countdownSeconds.takeIf { it > 0 },
        message = RecordingMessage.NONE,
        errorCode = null,
    )

    fun recordingCountdownChanged(state: RecordingState, seconds: Int?): RecordingState = state.copy(
        startCountdownSeconds = seconds,
    )

    fun recordingStartFailed(state: RecordingState): RecordingState = state.copy(
        isStarting = false,
        startCountdownSeconds = null,
    )
}
