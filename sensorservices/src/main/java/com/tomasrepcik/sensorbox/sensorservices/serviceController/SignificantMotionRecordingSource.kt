package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SignificantMotionRecording
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest

internal class SignificantMotionRecordingSource(
    private val request: RecordingRequest,
    private val recording: SignificantMotionRecording,
) : RecordingSource {
    override val type = RecordingSourceType.SIGNIFICANT_MOTION
    override val failures = recording.failures

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.SignificantMotion) {
            recording.start(request.folderName, request.useInternalStorage)
        } else {
            invalidSpec(type)
        }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> = recording.stop()
}
