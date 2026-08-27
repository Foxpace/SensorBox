package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSourceType

internal fun invalidSpec(type: RecordingSourceType): AppResult<Nothing> = AppResult.failure(
    AppError(AppErrorCode.VALIDATION, "Start $type recording source"),
)
