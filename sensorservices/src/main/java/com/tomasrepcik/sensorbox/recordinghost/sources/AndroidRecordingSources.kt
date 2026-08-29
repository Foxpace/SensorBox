package com.tomasrepcik.sensorbox.recordinghost.sources

import android.content.Context
import android.hardware.SensorManager
import com.google.android.gms.location.LocationServices
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recordinghost.request.RecordingRequest
import com.tomasrepcik.sensorbox.recordinghost.sources.activity.ActivityRecordingSource
import com.tomasrepcik.sensorbox.recordinghost.sources.activity.AndroidActivityRecognitionPlatform
import com.tomasrepcik.sensorbox.recordinghost.sources.gps.GPSHandler
import com.tomasrepcik.sensorbox.recordinghost.sources.gps.GpsRecording
import com.tomasrepcik.sensorbox.recordinghost.sources.gps.GpsRecordingSource
import com.tomasrepcik.sensorbox.recordinghost.sources.sensor.SensorRecording
import com.tomasrepcik.sensorbox.recordinghost.sources.sensor.SensorRecordingSource
import com.tomasrepcik.sensorbox.recordinghost.sources.sessionmetadata.SessionMetadataRecordingSource
import com.tomasrepcik.sensorbox.recordinghost.sources.significantmotion.SignificantMotionRecording
import com.tomasrepcik.sensorbox.recordinghost.sources.significantmotion.SignificantMotionRecordingSource
import com.tomasrepcik.sensorbox.recordinghost.storage.MeasurementStorage

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
