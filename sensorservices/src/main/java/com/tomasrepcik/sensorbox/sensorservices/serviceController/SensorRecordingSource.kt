package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SensorMeasurement

internal class SensorRecordingSource(
    private val config: MeasurementConfig,
    private val measurement: SensorMeasurement,
) : RecordingSource {
    override val type = RecordingSourceType.SENSOR

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> = if (spec is RecordingSourceSpec.Sensors) {
        measurement.prepare(
            folderName = config.folderName,
            useInternalStorage = config.useInternalStorage,
            sensorTypes = spec.sensorTypes,
            samplingPeriod = spec.samplingPeriod,
        ).flatMap { measurement.start() }
    } else {
        invalidSpec(type)
    }

    override suspend fun stop(): AppResult<Unit> = measurement.stop()
}
