package com.motionapps.sensorbox.recording

import com.motionapps.sensorbox.core.error.AppResult

interface RecordingSource {
    val type: RecordingSourceType

    suspend fun prepare(spec: RecordingSourceSpec): AppResult<Unit>

    suspend fun start(): AppResult<Unit>

    suspend fun stop(): AppResult<Unit>
}

fun interface RecordingClock {
    fun epochMillis(): Long
}

fun interface RecordingDelay {
    suspend fun pause(durationMillis: Long)
}
