package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SignificantMotion

internal class SignificantMotionRecordingSource(
    private val config: MeasurementConfig,
    private val measurement: SignificantMotion,
) : RecordingSource {
    override val type = RecordingSourceType.SIGNIFICANT_MOTION

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.SignificantMotion) {
            measurement.prepare(config.folderName, config.useInternalStorage).flatMap { measurement.start() }
        } else {
            invalidSpec(type)
        }

    override suspend fun stop(): AppResult<Unit> = measurement.stop()
}
