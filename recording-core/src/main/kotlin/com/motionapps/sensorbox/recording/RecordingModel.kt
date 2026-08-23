package com.motionapps.sensorbox.recording

import java.util.UUID

@JvmInline
value class RecordingSessionId(val value: String) {
    init {
        require(value.isNotBlank())
    }

    companion object {
        fun create(): RecordingSessionId = RecordingSessionId(UUID.randomUUID().toString())
    }
}

enum class RecordingSourceType {
    SESSION,
    SENSOR,
    GPS,
    ACTIVITY_RECOGNITION,
    SIGNIFICANT_MOTION,
}

sealed interface RecordingSourceSpec {
    val type: RecordingSourceType

    data object Session : RecordingSourceSpec {
        override val type = RecordingSourceType.SESSION
    }

    data class Sensors(val sensorTypes: Set<Int>, val samplingPeriod: Int) : RecordingSourceSpec {
        override val type = RecordingSourceType.SENSOR
    }

    data class Gps(val intervalSeconds: Int, val minimumDistanceMeters: Int) : RecordingSourceSpec {
        override val type = RecordingSourceType.GPS
    }

    data class ActivityRecognition(val periodSeconds: Int) : RecordingSourceSpec {
        override val type = RecordingSourceType.ACTIVITY_RECOGNITION
    }

    data object SignificantMotion : RecordingSourceSpec {
        override val type = RecordingSourceType.SIGNIFICANT_MOTION
    }
}

data class RecordingPlan(
    val sessionId: RecordingSessionId,
    val sources: List<RecordingSourceSpec>,
    val startAtEpochMillis: Long,
    val durationMillis: Long = 0L,
)

sealed interface RecordingSessionState {
    data object Idle : RecordingSessionState

    data class Preparing(val plan: RecordingPlan) : RecordingSessionState

    data class Prepared(val plan: RecordingPlan) : RecordingSessionState

    data class Running(val plan: RecordingPlan, val startedAtEpochMillis: Long) : RecordingSessionState

    data class Stopping(val sessionId: RecordingSessionId, val reason: RecordingStopReason) : RecordingSessionState
}

enum class RecordingStopReason {
    USER_REQUEST,
    DURATION_EXPIRED,
    LOW_BATTERY,
    SOURCE_FAILURE,
    PLATFORM_DESTROYED,
    PAIRED_ABORT,
}

sealed interface RecordingEvent {
    val sessionId: RecordingSessionId

    data class RecordingStarted(override val sessionId: RecordingSessionId, val startedAtEpochMillis: Long) :
        RecordingEvent

    data class RecordingStartRejected(
        override val sessionId: RecordingSessionId,
        val error: com.motionapps.sensorbox.core.error.AppError,
    ) : RecordingEvent

    data class RecordingStopped(
        override val sessionId: RecordingSessionId,
        val reason: RecordingStopReason,
        val result: com.motionapps.sensorbox.core.error.AppResult<Unit>,
    ) : RecordingEvent
}
