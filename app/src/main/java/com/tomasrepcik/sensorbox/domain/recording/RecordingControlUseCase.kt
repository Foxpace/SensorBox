package com.tomasrepcik.sensorbox.domain.recording

import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.domain.paired.PairedRecordingCoordinator
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearStopReason
import javax.inject.Inject

interface RecordingControlUseCase {
    suspend fun start(request: RecordingSetup): AppResult<Unit>

    suspend fun stop(): AppResult<Unit>

    fun annotate(text: String): AppResult<Unit>
}

class DefaultRecordingControlUseCase @Inject constructor(
    private val pairedRecordingCoordinator: PairedRecordingCoordinator,
    private val localController: PhoneRecordingController,
    private val clock: EpochClock,
) : RecordingControlUseCase {
    override suspend fun start(request: RecordingSetup): AppResult<Unit> = pairedRecordingCoordinator.start(request)

    override suspend fun stop(): AppResult<Unit> = pairedRecordingCoordinator.stop(WearStopReason.USER_REQUEST)

    override fun annotate(text: String): AppResult<Unit> = localController.annotate(text, clock.nowMillis())
}
