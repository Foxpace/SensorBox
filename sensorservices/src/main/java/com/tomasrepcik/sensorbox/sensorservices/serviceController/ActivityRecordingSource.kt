package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ActivityRecognitionPlatform
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.ActivityRecognitionRecording
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest

internal class ActivityRecordingSource(
    private val request: RecordingRequest,
    private val storage: MeasurementStorage,
    private val platform: ActivityRecognitionPlatform,
) : RecordingSource {
    private val recording = ActivityRecognitionRecording(storage, platform)
    override val type = RecordingSourceType.ACTIVITY_RECOGNITION
    override val failures = recording.failures

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.ActivityRecognition) {
            recording.start(
                folderName = request.folderName,
                useInternalStorage = request.useInternalStorage,
                periodSeconds = spec.periodSeconds,
            )
        } else {
            invalidSpec(type)
        }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> = recording.stop()
}
