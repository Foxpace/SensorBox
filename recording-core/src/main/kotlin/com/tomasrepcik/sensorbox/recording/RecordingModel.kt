package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppResult

@JvmInline
value class RecordingSessionId(val value: String) {
    init {
        require(value.isNotBlank())
    }
}

enum class RecordingSourceType {
    SESSION_METADATA,
    SENSOR,
    GPS,
    ACTIVITY_RECOGNITION,
    SIGNIFICANT_MOTION,
}

sealed interface RecordingSourceSpec {
    val type: RecordingSourceType

    data object SessionMetadata : RecordingSourceSpec {
        override val type = RecordingSourceType.SESSION_METADATA
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

data class RecordingRequest(
    val sessionId: RecordingSessionId,
    val sources: List<RecordingSourceSpec>,
    val durationMillis: Long = 0L,
)

enum class RecordingStopReason {
    USER_REQUEST,
    DURATION_EXPIRED,
    LOW_BATTERY,
    SOURCE_FAILURE,
    PLATFORM_DESTROYED,
}

data class RecordingStopContext(val reason: RecordingStopReason, val failures: List<AppError> = emptyList()) {
    val failure: AppError?
        get() = failures.firstOrNull()

    fun withFailure(failure: AppError): RecordingStopContext = copy(failures = failures + failure)
}

sealed interface RecordingEvent {
    val sessionId: RecordingSessionId

    data class RecordingStarted(override val sessionId: RecordingSessionId) : RecordingEvent

    data class SourceFailed(
        override val sessionId: RecordingSessionId,
        val sourceType: RecordingSourceType,
        val failure: AppError,
        val stopFailure: AppError? = null,
    ) : RecordingEvent

    data class RecordingStopped(
        override val sessionId: RecordingSessionId,
        val reason: RecordingStopReason,
        val result: AppResult<Unit>,
    ) : RecordingEvent
}
