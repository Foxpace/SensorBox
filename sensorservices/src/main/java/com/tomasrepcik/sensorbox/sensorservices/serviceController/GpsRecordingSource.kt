package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.GpsRecording
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest

internal class GpsRecordingSource(private val request: RecordingRequest, private val recording: GpsRecording) :
    RecordingSource {
    override val type = RecordingSourceType.GPS
    override val failures = recording.failures

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> = if (spec is RecordingSourceSpec.Gps) {
        recording.start(
            folderName = request.folderName,
            useInternalStorage = request.useInternalStorage,
            intervalSeconds = spec.intervalSeconds,
            minimumDistanceMeters = spec.minimumDistanceMeters,
        )
    } else {
        invalidSpec(type)
    }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> = recording.stop()
}
