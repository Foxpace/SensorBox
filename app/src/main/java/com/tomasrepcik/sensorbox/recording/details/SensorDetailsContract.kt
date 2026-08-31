package com.tomasrepcik.sensorbox.recording.details

import com.tomasrepcik.sensorbox.recording.RecordingDevice
import com.tomasrepcik.sensorbox.recording.RecordingScreenEffect
import com.tomasrepcik.sensorbox.recording.RecordingState

sealed interface SensorDetailsIntent {
    data class LoadDetails(val sensorType: Int?, val device: RecordingDevice) : SensorDetailsIntent
    data object OpenSensorPreview : SensorDetailsIntent
    data object StartGpsPreview : SensorDetailsIntent
    data object StopPreview : SensorDetailsIntent
    data object RequestLocationPreviewPermission : SensorDetailsIntent
}

sealed interface SensorDetailsEffect : RecordingScreenEffect {
    data class OpenSensorPreview(val sensorType: Int?, val device: RecordingDevice) : SensorDetailsEffect
    data object RequestLocationPreviewPermission : SensorDetailsEffect
}

object SensorDetailsReducer {
    fun reduce(state: RecordingState, intent: SensorDetailsIntent): RecordingState = when (intent) {
        is SensorDetailsIntent.LoadDetails -> state.copy(
            detailsSensorType = intent.sensorType,
            detailsDevice = intent.device,
        )

        SensorDetailsIntent.OpenSensorPreview,
        SensorDetailsIntent.StartGpsPreview,
        SensorDetailsIntent.StopPreview,
        SensorDetailsIntent.RequestLocationPreviewPermission,
        -> state
    }
}
