package com.tomasrepcik.sensorbox.recording.active

import com.tomasrepcik.sensorbox.navigation.MainRoute
import com.tomasrepcik.sensorbox.recording.RecordingDevice
import com.tomasrepcik.sensorbox.recording.RecordingDraft
import com.tomasrepcik.sensorbox.recording.RecordingMessage
import com.tomasrepcik.sensorbox.recording.RecordingScreenEffect
import com.tomasrepcik.sensorbox.recording.RecordingState
import com.tomasrepcik.sensorbox.recording.toDraft

sealed interface RecordIntent {
    data class Navigate(val route: MainRoute) : RecordIntent
    data class ToggleSensor(val sensorId: Int) : RecordIntent
    data class ToggleWatchSensor(val sensorId: Int) : RecordIntent
    data class OpenSensorDetails(val sensorType: Int?, val device: RecordingDevice = RecordingDevice.PHONE) :
        RecordIntent
    data object ToggleGps : RecordIntent
    data object ToggleWatchGps : RecordIntent
    data object OpenRecordingSetup : RecordIntent
    data object ClearMessage : RecordIntent
}

sealed interface RecordEffect : RecordingScreenEffect {
    data class Navigate(val route: MainRoute) : RecordEffect
    data class OpenSensorDetails(val sensorType: Int?, val device: RecordingDevice) : RecordEffect
    data class OpenRecordingSetup(val draft: RecordingDraft) : RecordEffect
}

data class RecordNext(val state: RecordingState, val effect: RecordEffect? = null)

object RecordReducer {
    fun reduce(state: RecordingState, intent: RecordIntent): RecordNext = when (intent) {
        is RecordIntent.Navigate -> RecordNext(state, RecordEffect.Navigate(intent.route))

        is RecordIntent.OpenSensorDetails -> RecordNext(
            state,
            RecordEffect.OpenSensorDetails(intent.sensorType, intent.device),
        )

        RecordIntent.OpenRecordingSetup -> RecordNext(
            state.copy(message = RecordingMessage.NONE),
            RecordEffect.OpenRecordingSetup(state.toDraft()),
        )

        RecordIntent.ClearMessage -> RecordNext(state.copy(message = RecordingMessage.NONE, errorCode = null))

        RecordIntent.ToggleGps -> RecordNext(state.copy(includesGps = !state.includesGps))

        RecordIntent.ToggleWatchGps -> RecordNext(state.copy(watchIncludesGps = !state.watchIncludesGps))

        is RecordIntent.ToggleSensor -> RecordNext(state.toggleSensor(intent.sensorId))

        is RecordIntent.ToggleWatchSensor -> RecordNext(state.toggleWatchSensor(intent.sensorId))
    }

    private fun RecordingState.toggleSensor(sensorId: Int): RecordingState {
        val updated = selectedSensorIds.toMutableSet()
        if (!updated.add(sensorId)) updated.remove(sensorId)
        return copy(selectedSensorIds = updated, message = RecordingMessage.NONE, errorCode = null)
    }

    private fun RecordingState.toggleWatchSensor(sensorId: Int): RecordingState {
        val updated = selectedWatchSensorIds.toMutableSet()
        if (!updated.add(sensorId)) updated.remove(sensorId)
        return copy(selectedWatchSensorIds = updated, message = RecordingMessage.NONE, errorCode = null)
    }
}
