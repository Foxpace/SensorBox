package com.tomasrepcik.sensorbox.recordinghost.sources.activity

import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingRequest
import com.tomasrepcik.sensorbox.recordinghost.sources.activity.ActivityRecognitionPlatform
import com.tomasrepcik.sensorbox.recordinghost.sources.activity.ActivityRecognitionRecording
import com.tomasrepcik.sensorbox.recordinghost.sources.invalidSpec
import com.tomasrepcik.sensorbox.recordinghost.storage.MeasurementStorage

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
