package com.tomasrepcik.sensorbox.recording

import kotlinx.serialization.Serializable

@Serializable
data class RecordingDraft(
    val detailsSensorType: Int? = null,
    val detailsDevice: RecordingDevice = RecordingDevice.PHONE,
    val selectedSensorIds: Set<Int> = emptySet(),
    val includesGps: Boolean = false,
    val selectedWatchSensorIds: Set<Int> = emptySet(),
    val watchIncludesGps: Boolean = false,
    val customMeasurementName: String = "",
    val startDelaySeconds: Int = 0,
    val durationSeconds: Int = 0,
    val notes: String = "",
    val alarmOffsets: String = "",
    val activityRecognition: Boolean = false,
    val activityRecognitionPeriodSeconds: Int = 30,
    val significantMotion: Boolean = false,
)

fun RecordingState.withDraft(draft: RecordingDraft): RecordingState = copy(
    detailsSensorType = draft.detailsSensorType,
    detailsDevice = draft.detailsDevice,
    selectedSensorIds = draft.selectedSensorIds,
    includesGps = draft.includesGps,
    selectedWatchSensorIds = draft.selectedWatchSensorIds,
    watchIncludesGps = draft.watchIncludesGps,
    customMeasurementName = draft.customMeasurementName,
    startDelaySeconds = draft.startDelaySeconds,
    durationSeconds = draft.durationSeconds,
    notes = draft.notes,
    alarmOffsets = draft.alarmOffsets,
    activityRecognition = draft.activityRecognition,
    activityRecognitionPeriodSeconds = draft.activityRecognitionPeriodSeconds,
    significantMotion = draft.significantMotion,
)

fun RecordingState.toDraft() = RecordingDraft(
    detailsSensorType = detailsSensorType,
    detailsDevice = detailsDevice,
    selectedSensorIds = selectedSensorIds,
    includesGps = includesGps,
    selectedWatchSensorIds = selectedWatchSensorIds,
    watchIncludesGps = watchIncludesGps,
    customMeasurementName = customMeasurementName,
    startDelaySeconds = startDelaySeconds,
    durationSeconds = durationSeconds,
    notes = notes,
    alarmOffsets = alarmOffsets,
    activityRecognition = activityRecognition,
    activityRecognitionPeriodSeconds = activityRecognitionPeriodSeconds,
    significantMotion = significantMotion,
)
