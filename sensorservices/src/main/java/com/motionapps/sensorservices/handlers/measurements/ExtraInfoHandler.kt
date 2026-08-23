package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.SystemClock
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorbox.core.time.EpochClock
import com.motionapps.sensorservices.handlers.MeasurementStorage
import com.motionapps.sensorservices.serviceController.MeasurementConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Collects session metadata and writes it once when the recording is closed. */
internal class ExtraInfoHandler(private val storage: MeasurementStorage, private val clock: EpochClock) {
    private var config: MeasurementConfig? = null
    private var startedAtMillis: Long = 0L
    private var startedAtNanos: Long = 0L
    private val annotations = mutableListOf<MeasurementAnnotation>()
    private val triggeredAlarms = mutableListOf<Long>()
    private var written = false

    fun start(config: MeasurementConfig) {
        this.config = config
        startedAtMillis = clock.nowMillis()
        startedAtNanos = SystemClock.elapsedRealtimeNanos()
        annotations.clear()
        triggeredAlarms.clear()
        written = false
    }

    fun annotate(timestampMillis: Long, text: String) {
        text.trim().takeIf(String::isNotEmpty)?.let { annotations += MeasurementAnnotation(timestampMillis, it) }
    }

    fun alarmTriggered(timestampMillis: Long = clock.nowMillis()) {
        triggeredAlarms += timestampMillis
    }

    fun write(context: Context): AppResult<Unit> {
        val active = config ?: return AppResult.success(Unit)
        if (written) return AppResult.success(Unit)
        written = true
        val metadataResult = appResult(AppErrorCode.MEASUREMENT, "Build measurement metadata") {
            MeasurementMetadata(
                millis = startedAtMillis,
                nanos = startedAtNanos,
                type = active.measurementType,
                date = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date(startedAtMillis)),
                folder = active.folderName,
                notes = active.notes,
                annotations = annotations.toList(),
                ranges = sensorRanges(context, active),
                alarms = triggeredAlarms.toList(),
                configuredAlarmOffsetsSeconds = active.alarmOffsetsSeconds.toList(),
                durationMillis = active.durationMillis,
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
        }.withAppError(AppErrorCode.MEASUREMENT, "Write measurement metadata")
    }

    private fun sensorRanges(context: Context, active: MeasurementConfig): List<SensorRange> {
        val manager = context.getSystemService(SensorManager::class.java)
        val rangeIds = if (active.significantMotion) {
            active.sensorIds + Sensor.TYPE_SIGNIFICANT_MOTION
        } else {
            active.sensorIds
        }
        return rangeIds.distinct().mapNotNull { type ->
            manager.getDefaultSensor(type)?.let { sensor ->
                SensorRange(sensor = sensor.name, type = type, range = sensor.maximumRange)
            }
        }
    }

    private companion object {
        const val DATE_FORMAT = "dd. MM. yyyy HH:mm:ss"
        const val EXTRA_FILE = "extra.json"
        val JSON = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
        }
    }
}

@Serializable
internal data class MeasurementMetadata(
    val millis: Long,
    val nanos: Long,
    val type: String,
    val date: String,
    val folder: String,
    val notes: List<String>,
    val annotations: List<MeasurementAnnotation>,
    val ranges: List<SensorRange>,
    val alarms: List<Long>,
    val configuredAlarmOffsetsSeconds: List<Int>,
    val durationMillis: Long,
    val activityRecognition: Boolean,
    val significantMotion: Boolean,
)

@Serializable
internal data class MeasurementAnnotation(val timestamp: Long, val annotation: String)

@Serializable
internal data class SensorRange(val sensor: String, val type: Int, val range: Float)
