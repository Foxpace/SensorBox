package com.tomasrepcik.sensorbox.sensorservices.serviceController

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.recording.RecordingSource
import com.tomasrepcik.sensorbox.recording.RecordingSourceSpec
import com.tomasrepcik.sensorbox.recording.RecordingSourceType

internal class SessionRecordingSource(private val artifacts: SessionArtifacts) : RecordingSource {
    override val type = RecordingSourceType.SESSION

    override suspend fun start(spec: RecordingSourceSpec): AppResult<Unit> =
        if (spec is RecordingSourceSpec.Session) artifacts.start() else invalidSpec(type)

    override suspend fun stop(): AppResult<Unit> = artifacts.stop()
}
