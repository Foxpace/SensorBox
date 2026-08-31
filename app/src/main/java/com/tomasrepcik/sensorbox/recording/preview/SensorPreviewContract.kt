package com.tomasrepcik.sensorbox.recording.preview

import com.tomasrepcik.sensorbox.recording.RecordingDevice
import com.tomasrepcik.sensorbox.recording.RecordingScreenEffect
import com.tomasrepcik.sensorbox.recording.RecordingState

sealed interface SensorPreviewIntent {
    data class LoadDetails(val sensorType: Int?, val device: RecordingDevice) : SensorPreviewIntent
    data object StartGpsPreview : SensorPreviewIntent
    data class StartSensorPreview(val sensorType: Int) : SensorPreviewIntent
    data object StopPreview : SensorPreviewIntent
    data object RequestLocationPreviewPermission : SensorPreviewIntent
}

sealed interface SensorPreviewEffect : RecordingScreenEffect {
    data object RequestLocationPreviewPermission : SensorPreviewEffect
}

object SensorPreviewReducer {
    fun reduce(state: RecordingState, intent: SensorPreviewIntent): RecordingState = when (intent) {
        is SensorPreviewIntent.LoadDetails -> state.copy(
            detailsSensorType = intent.sensorType,
            detailsDevice = intent.device,
        )

        SensorPreviewIntent.StartGpsPreview,
        is SensorPreviewIntent.StartSensorPreview,
        SensorPreviewIntent.StopPreview,
        SensorPreviewIntent.RequestLocationPreviewPermission,
        -> state
    }
}
