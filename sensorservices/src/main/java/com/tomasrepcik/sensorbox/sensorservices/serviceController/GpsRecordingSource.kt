package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.GPSMeasurement

internal class GpsRecordingSource(private val config: MeasurementConfig, private val measurement: GPSMeasurement) :
    RecordingSource {
    override val type = RecordingSourceType.GPS

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> = if (spec is RecordingSourceSpec.Gps) {
        measurement.prepare(
            folderName = config.folderName,
            useInternalStorage = config.useInternalStorage,
            intervalSeconds = spec.intervalSeconds,
            minimumDistanceMeters = spec.minimumDistanceMeters,
        ).flatMap { measurement.start() }
    } else {
        invalidSpec(type)
    }

    override suspend fun stop(): AppResult<Unit> = measurement.stop()
}
