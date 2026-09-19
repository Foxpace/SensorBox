package com.tomasrepcik.sensorbox.recording

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface RecordingSource {
    val type: RecordingSourceType
    val failures: Flow<AppError>
        get() = emptyFlow()

    suspend fun start(spec: RecordingSourceSpec): AppResult<Unit>

    suspend fun stop(context: RecordingStopContext): AppResult<Unit>
}
