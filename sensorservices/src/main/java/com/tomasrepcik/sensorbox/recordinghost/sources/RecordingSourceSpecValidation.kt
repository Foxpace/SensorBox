package com.tomasrepcik.sensorbox.recordinghost.sources

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSourceType

internal fun invalidSpec(type: RecordingSourceType): AppResult<Nothing> = AppResult.failure(
    AppError(AppErrorCode.VALIDATION, "Start $type recording source"),
)
