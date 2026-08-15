package com.motionapps.sensorservices.handlers.measurements

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.SystemClock
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.appResult
import com.motionapps.sensorbox.core.error.flatMap
import com.motionapps.sensorbox.core.error.withAppError
import com.motionapps.sensorservices.handlers.StorageHandler
import com.motionapps.sensorservices.serviceController.MeasurementConfig
import org.json.JSONArray
import org.json.JSONObject

/** Collects session metadata and writes it once when the recording is closed. */
class ExtraInfoHandler {
    private var config: MeasurementConfig? = null
    private var startedAtMillis: Long = 0L
    private var startedAtNanos: Long = 0L
    private val annotations = mutableListOf<Annotation>()
    private val triggeredAlarms = mutableListOf<Long>()
    private var written = false

    fun start(config: MeasurementConfig) {
        this.config = config
        startedAtMillis = System.currentTimeMillis()
        startedAtNanos = SystemClock.elapsedRealtimeNanos()
        annotations.clear()
        triggeredAlarms.clear()
        written = false
    }

    fun annotate(timestampMillis: Long, text: String) {
        text.trim().takeIf(String::isNotEmpty)?.let { annotations += Annotation(timestampMillis, it) }
    }

    fun alarmTriggered(timestampMillis: Long = System.currentTimeMillis()) {
        triggeredAlarms += timestampMillis
    }

    fun write(context: Context): Result<Unit> {
        val active = config ?: return Result.success(Unit)
        if (written) return Result.success(Unit)
        written = true
        val jsonResult = appResult(AppError.Kind.MEASUREMENT, "Build measurement metadata") {
            JSONObject().apply {
                put("millis", startedAtMillis)
                put("nanos", startedAtNanos)
                put("type", active.measurementType)
                put("date", StorageHandler.getDate(startedAtMillis))
                put("folder", active.folderName)
                put("notes", JSONArray(active.notes))
                put(
                    "annotations",
                    JSONArray().apply {
                        annotations.forEach { annotation ->
                            put(
                                JSONObject()
                                    .put("timestamp", annotation.timestampMillis)
                                    .put("annotation", annotation.text),
                            )
                        }
                    },
                )
                val rangeIds = if (active.significantMotion) {
                    active.sensorIds + Sensor.TYPE_SIGNIFICANT_MOTION
                } else {
                    active.sensorIds
                }
                put("ranges", sensorRanges(context, rangeIds))
                put("alarms", JSONArray(triggeredAlarms))
                put("configuredAlarmOffsetsSeconds", JSONArray(active.alarmOffsetsSeconds))
                put("durationMillis", active.durationMillis)
                put("activityRecognition", active.activityRecognition)
                put("significantMotion", active.significantMotion)
            }
        }
        return jsonResult.flatMap { json ->
            val stream = if (active.useInternalStorage) {
                StorageHandler.createFileInInternalFolder(context, active.folderName, EXTRA_FILE)
            } else {
                StorageHandler.createFileInFolder(context, active.folderName, "application/json", EXTRA_FILE)
            }
            stream.flatMap { output ->
                appResult(AppError.Kind.STORAGE, "Write measurement metadata file") {
                    output.use { it.write(json.toString(2).toByteArray()) }
                }
            }
        }.withAppError(AppError.Kind.MEASUREMENT, "Write measurement metadata")
    }

    private fun sensorRanges(context: Context, sensorIds: IntArray): JSONArray {
        val manager = context.getSystemService(SensorManager::class.java)
        return JSONArray().apply {
            sensorIds.distinct().forEach { type ->
                manager.getDefaultSensor(type)?.let { sensor ->
                    put(
                        JSONObject()
                            .put("sensor", sensor.name)
                            .put("type", type)
                            .put("range", sensor.maximumRange),
                    )
                }
            }
        }
    }

    private data class Annotation(val timestampMillis: Long, val text: String)

    private companion object {
        const val EXTRA_FILE = "extra.json"
    }
}
