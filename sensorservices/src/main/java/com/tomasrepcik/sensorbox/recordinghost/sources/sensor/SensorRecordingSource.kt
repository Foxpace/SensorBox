package com.tomasrepcik.sensorbox.recordinghost.sources.sensor

import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingRequest
import com.tomasrepcik.sensorbox.recordinghost.sources.invalidSpec
import com.tomasrepcik.sensorbox.recordinghost.sources.sensor.SensorRecording

internal class SensorRecordingSource(private val request: RecordingRequest, private val recording: SensorRecording) :
    RecordingSource {
    override val type = RecordingSourceType.SENSOR
    override val failures = recording.failures

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> = if (spec is RecordingSourceSpec.Sensors) {
        recording.start(
            folderName = request.folderName,
            useInternalStorage = request.useInternalStorage,
            sensorTypes = spec.sensorTypes,
            samplingPeriod = spec.samplingPeriod,
        )
    } else {
        invalidSpec(type)
    }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> = recording.stop()
}
