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
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.GpsRecording
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SensorRecording
import com.tomasrepcik.sensorbox.sensorservices.handlers.measurements.SignificantMotionRecording
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest

internal class AndroidRecordingSources(
    context: Context,
    request: RecordingRequest,
    storage: MeasurementStorage,
    diagnosticLogger: DiagnosticLogger,
    clock: EpochClock,
) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sessionMetadata = SessionMetadataRecordingSource(request, storage, clock, sensorManager)

    val sources: List<RecordingSource> = listOf(
        sessionMetadata,
        SensorRecordingSource(
            request,
            SensorRecording(
                storage = storage,
                diagnosticLogger = diagnosticLogger,
                sensorManager = sensorManager,
                onStopped = sessionMetadata::recordSensorStats,
            ),
        ),
        GpsRecordingSource(
            request,
            GpsRecording(GPSHandler(LocationServices.getFusedLocationProviderClient(context)), storage, clock),
        ),
        ActivityRecordingSource(
            request,
            storage,
            AndroidActivityRecognitionPlatform(context.applicationContext),
        ),
        SignificantMotionRecordingSource(
            request,
            SignificantMotionRecording(storage, sensorManager),
        ),
    )

    fun annotate(timestampMillis: Long, text: String): AppResult<Unit> = sessionMetadata.annotate(timestampMillis, text)

    fun playAlarm(): AppResult<Unit> = sessionMetadata.playAlarm()
}
