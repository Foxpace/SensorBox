package com.tomasrepcik.sensorbox.sensorservices.serviceController

import android.content.Context
import android.hardware.SensorManager
import com.google.android.gms.location.LocationServices
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.sensorservices.handlers.GPSHandler
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.AndroidActivityRecognitionPlatform
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.GPSMeasurement
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SensorMeasurement
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SignificantMotion

internal class AndroidRecordingSources(
    context: Context,
    config: MeasurementConfig,
    storage: MeasurementStorage,
    diagnosticLogger: DiagnosticLogger,
    clock: EpochClock,
) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val artifacts = SessionArtifacts(config, storage, clock, sensorManager)

    val sources: List<RecordingSource> = listOf(
        SessionRecordingSource(artifacts),
        SensorRecordingSource(
            config,
            SensorMeasurement(storage, diagnosticLogger, clock, sensorManager),
        ),
        GpsRecordingSource(
            config,
            GPSMeasurement(GPSHandler(LocationServices.getFusedLocationProviderClient(context)), storage, clock),
        ),
        ActivityRecordingSource(
            config,
            storage,
            AndroidActivityRecognitionPlatform(context.applicationContext),
        ),
        SignificantMotionRecordingSource(
            config,
            SignificantMotion(storage, clock, sensorManager),
        ),
    )

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = artifacts.annotate(timestampMillis, text)

    fun playAlarm(): AppResult<Unit> = artifacts.playAlarm()
}
