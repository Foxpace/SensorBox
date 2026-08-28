package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ActivityRecognitionMeasurement
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ActivityRecognitionPlatform
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementLaunchRequest

internal class ActivityRecordingSource(
    private val request: MeasurementLaunchRequest,
    private val storage: MeasurementStorage,
    private val platform: ActivityRecognitionPlatform,
) : RecordingSource {
    private val measurement = ActivityRecognitionMeasurement(storage, platform)
    override val type = RecordingSourceType.ACTIVITY_RECOGNITION
    override val failures = measurement.failures

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.ActivityRecognition) {
            measurement.start(
                folderName = request.folderName,
                useInternalStorage = request.useInternalStorage,
                periodSeconds = spec.periodSeconds,
            )
        } else {
            invalidSpec(type)
        }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> = measurement.stop()
}
