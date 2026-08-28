package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SignificantMotion
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementLaunchRequest

internal class SignificantMotionRecordingSource(
    private val request: MeasurementLaunchRequest,
    private val measurement: SignificantMotion,
) : RecordingSource {
    override val type = RecordingSourceType.SIGNIFICANT_MOTION
    override val failures = measurement.failures

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.SignificantMotion) {
            measurement.start(request.folderName, request.useInternalStorage)
        } else {
            invalidSpec(type)
        }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> = measurement.stop()
}
