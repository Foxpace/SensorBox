package com.tomasrepcik.sensorbox.measurements.preview

import androidx.annotation.StringRes
import com.google.android.gms.location.DetectedActivity
import com.tomasrepcik.sensorbox.R

internal enum class RecordedEventKind { STEP, MOTION, ACTIVITY_TRANSITION }

internal fun recordedEventKind(columns: List<String>): RecordedEventKind? = when {
    columns == listOf("step") -> RecordedEventKind.STEP
    columns == listOf("event") -> RecordedEventKind.MOTION
    columns.toSet() == setOf("activity", "enter_exit") -> RecordedEventKind.ACTIVITY_TRANSITION
    else -> null
}

internal fun isActivityColumn(column: String): Boolean = column in activityNames

@StringRes
internal fun activityNameResource(activity: String): Int = activityNames[activity] ?: R.string.preview_activity_unknown

@StringRes
internal fun activityNameResource(activity: Double?): Int = when (activity) {
    DetectedActivity.STILL.toDouble() -> R.string.preview_activity_still
    DetectedActivity.ON_FOOT.toDouble() -> R.string.preview_activity_on_foot
    DetectedActivity.WALKING.toDouble() -> R.string.preview_activity_walking
    DetectedActivity.RUNNING.toDouble() -> R.string.preview_activity_running
    DetectedActivity.IN_VEHICLE.toDouble() -> R.string.preview_activity_vehicle
    DetectedActivity.ON_BICYCLE.toDouble() -> R.string.preview_activity_bike
    DetectedActivity.TILTING.toDouble() -> R.string.preview_activity_tilting
    else -> R.string.preview_activity_unknown
}

private val activityNames = mapOf(
    "still" to R.string.preview_activity_still,
    "on_foot" to R.string.preview_activity_on_foot,
    "walking" to R.string.preview_activity_walking,
    "running" to R.string.preview_activity_running,
    "vehicle" to R.string.preview_activity_vehicle,
    "bike" to R.string.preview_activity_bike,
    "unknown" to R.string.preview_activity_unknown,
    "tilting" to R.string.preview_activity_tilting,
)
