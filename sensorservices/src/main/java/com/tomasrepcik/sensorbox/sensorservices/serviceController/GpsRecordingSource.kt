package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.GPSMeasurement
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementLaunchRequest

internal class GpsRecordingSource(
    private val request: MeasurementLaunchRequest,
    private val measurement: GPSMeasurement,
) : RecordingSource {
    override val type = RecordingSourceType.GPS
    override val failures = measurement.failures

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> = if (spec is RecordingSourceSpec.Gps) {
        measurement.start(
            folderName = request.folderName,
            useInternalStorage = request.useInternalStorage,
            intervalSeconds = spec.intervalSeconds,
            minimumDistanceMeters = spec.minimumDistanceMeters,
        )
    } else {
        invalidSpec(type)
    }

    override suspend fun stop(context: RecordingStopContext): AppResult<Unit> = measurement.stop()
}
