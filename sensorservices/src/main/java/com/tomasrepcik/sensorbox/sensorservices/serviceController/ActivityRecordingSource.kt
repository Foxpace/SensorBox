package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ActivityRecognitionMeasurement
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ActivityRecognitionPlatform

internal class ActivityRecordingSource(
    private val config: MeasurementConfig,
    private val storage: MeasurementStorage,
    private val platform: ActivityRecognitionPlatform,
) : RecordingSource {
    private var measurement: ActivityRecognitionMeasurement? = null
    override val type = RecordingSourceType.ACTIVITY_RECOGNITION

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.ActivityRecognition) {
            val recording = ActivityRecognitionMeasurement(spec.periodSeconds, storage, platform)
            measurement = recording
            recording.prepare(config.folderName, config.useInternalStorage).flatMap { recording.start() }
        } else {
            invalidSpec(type)
        }

    override suspend fun stop(): AppResult<Unit> {
        val result = measurement?.stop() ?: AppResult.success(Unit)
        measurement = null
        return result
    }
}
