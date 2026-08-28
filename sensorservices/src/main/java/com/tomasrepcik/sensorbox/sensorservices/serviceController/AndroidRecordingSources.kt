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
import com.tomasrepcik.sensorbox.sensorservices.intent.MeasurementLaunchRequest

internal class AndroidRecordingSources(
    context: Context,
    request: MeasurementLaunchRequest,
    storage: MeasurementStorage,
    diagnosticLogger: DiagnosticLogger,
    clock: EpochClock,
) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val session = SessionRecordingSource(request, storage, clock, sensorManager)

    val sources: List<RecordingSource> = listOf(
        session,
        SensorRecordingSource(
            request,
            SensorMeasurement(
                storage = storage,
                diagnosticLogger = diagnosticLogger,
                sensorManager = sensorManager,
                onStopped = session::recordSensorStats,
            ),
        ),
        GpsRecordingSource(
            request,
            GPSMeasurement(GPSHandler(LocationServices.getFusedLocationProviderClient(context)), storage, clock),
        ),
        ActivityRecordingSource(
            request,
            storage,
            AndroidActivityRecognitionPlatform(context.applicationContext),
        ),
        SignificantMotionRecordingSource(
            request,
            SignificantMotion(storage, sensorManager),
        ),
    )

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = session.annotate(timestampMillis, text)

    fun playAlarm(): AppResult<Unit> = session.playAlarm()
}
