package com.tomasrepcik.sensorbox.sensorservices.handlers.measurements

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.SystemClock
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.appResult
import com.tomasrepcik.sensorbox.core.error.flatMap
import com.tomasrepcik.sensorbox.core.error.withAppError
import com.tomasrepcik.sensorbox.core.time.ClockFormats
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.recording.RecordingStopContext
import com.tomasrepcik.sensorbox.sensorservices.handlers.MeasurementStorage
import com.tomasrepcik.sensorbox.sensorservices.intent.RecordingRequest
import com.tomasrepcik.sensorbox.sensorservices.types.SensorFileStats
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Collects session metadata and writes it once when the recording is closed. */
internal class MeasurementMetadataWriter(
    private val storage: MeasurementStorage,
    private val clock: EpochClock,
    private val sensorManager: SensorManager,
) {
    private var request: RecordingRequest? = null
    private var startedAtMillis: Long = 0L
    private var startedAtNanos: Long = 0L
    private var sensorStats: List<SensorFileStats> = emptyList()
    private val annotations = mutableListOf<MeasurementAnnotation>()
    private val triggeredAlarms = mutableListOf<Long>()

    fun start(request: RecordingRequest) {
        this.request = request
        val startedAt = capturePhoneTime()
        startedAtMillis = startedAt.unixMillis
        startedAtNanos = startedAt.elapsedRealtimeNanos
        annotations.clear()
        triggeredAlarms.clear()
        sensorStats = emptyList()
    }

    fun annotate(timestampMillis: Long, text: String) {
        text.trim().takeIf(String::isNotEmpty)?.let { annotations += MeasurementAnnotation(timestampMillis, it) }
    }

    fun alarmTriggered(timestampMillis: Long = clock.nowMillis()) {
        triggeredAlarms += timestampMillis
    }

    fun recordSensorStats(stats: List<SensorFileStats>) {
        sensorStats = stats
    }

    fun write(context: RecordingStopContext): AppResult<Unit> {
        val active = request ?: return AppResult.success(Unit)
        request = null
        val endedAt = capturePhoneTime()
        val metadataResult = appResult(AppErrorCode.RECORDING, "Build measurement metadata") {
            MeasurementMetadata(
                millis = startedAtMillis,
                nanos = startedAtNanos,
                endedAtMillis = endedAt.unixMillis,
                endedAtNanos = endedAt.elapsedRealtimeNanos,
                type = RECORDING_TYPE,
                date = ClockFormats.metadataTimestamp(startedAtMillis),
                folder = active.folderName,
                notes = active.notes,
                annotations = annotations.toList(),
                ranges = sensorRanges(active),
                alarms = triggeredAlarms.toList(),
                configuredAlarmOffsetsSeconds = active.alarmOffsetsSeconds.toList(),
                durationMillis = active.durationMillis,
                actualDurationMillis = ((endedAt.elapsedRealtimeNanos - startedAtNanos) / NANOS_PER_MILLISECOND)
                    .coerceAtLeast(0L),
                stopReason = context.reason.name,
                failureOperation = context.failure?.operation,
                failureMessage = context.failure?.diagnosticMessage,
                failures = context.failures.map(AppError::toMeasurementFailure),
                wakeLockEnabled = active.requiresWakeLock,
                sensorFiles = sensorStats,
                activityRecognition = active.activityRecognition,
                significantMotion = active.significantMotion,
            )
        }
        return metadataResult.flatMap { metadata ->
            storage.openMeasurementFile(
                folderName = active.folderName,
                mimeType = "application/json",
                fileName = EXTRA_FILE,
                useInternalStorage = active.useInternalStorage,
            ).flatMap { output ->
                appResult(AppErrorCode.STORAGE, "Write measurement metadata file") {
                    output.use { it.write(JSON.encodeToString(metadata).toByteArray()) }
                }
            }
        }.withAppError(AppErrorCode.RECORDING, "Write measurement metadata")
    }

    private fun sensorRanges(active: RecordingRequest): List<SensorRange> {
        val rangeIds = if (active.significantMotion) {
            active.sensorIds + Sensor.TYPE_SIGNIFICANT_MOTION
        } else {
            active.sensorIds
        }
        return rangeIds.distinct().mapNotNull { type ->
            sensorManager.getDefaultSensor(type)?.let { sensor ->
                SensorRange(sensor = sensor.name, type = type, range = sensor.maximumRange)
            }
        }
    }

    private fun capturePhoneTime(): PhoneTime {
        val before = SystemClock.elapsedRealtimeNanos()
        val unixMillis = clock.nowMillis()
        val after = SystemClock.elapsedRealtimeNanos()
        return PhoneTime(
            unixMillis = unixMillis,
            elapsedRealtimeNanos = before + (after - before) / 2,
        )
    }

    private companion object {
        const val EXTRA_FILE = "extra.json"
        const val RECORDING_TYPE = "RECORDING"
        const val NANOS_PER_MILLISECOND = 1_000_000L
        val JSON = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
        }
    }

    private data class PhoneTime(val unixMillis: Long, val elapsedRealtimeNanos: Long)
}

@Serializable
internal data class MeasurementMetadata(
    val millis: Long,
    val nanos: Long,
    val endedAtMillis: Long,
    val endedAtNanos: Long,
    val type: String,
    val date: String,
    val folder: String,
    val notes: List<String>,
    val annotations: List<MeasurementAnnotation>,
    val ranges: List<SensorRange>,
    val alarms: List<Long>,
    val configuredAlarmOffsetsSeconds: List<Int>,
    val durationMillis: Long,
    val actualDurationMillis: Long,
    val stopReason: String,
    val failureOperation: String?,
    val failureMessage: String?,
    val failures: List<MeasurementFailure>,
    val wakeLockEnabled: Boolean,
    val sensorFiles: List<SensorFileStats>,
    val activityRecognition: Boolean,
    val significantMotion: Boolean,
)

@Serializable
internal data class MeasurementAnnotation(val timestamp: Long, val annotation: String)

@Serializable
internal data class SensorRange(val sensor: String, val type: Int, val range: Float)

@Serializable
internal data class MeasurementFailure(
    val code: String,
    val operation: String,
    val message: String,
    val cause: String?,
    val context: Map<String, String>,
)

private fun AppError.toMeasurementFailure() = MeasurementFailure(
    code = code.name,
    operation = operation,
    message = diagnosticMessage,
    cause = cause?.let { error -> "${error::class.java.simpleName}: ${error.message.orEmpty()}" },
    context = context,
)
