package com.tomasrepcik.sensorbox.domain.measurement

import com.motionapps.wearoslib.protocol.WearStopReason
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.domain.paired.PairedRecordingCoordinator
import javax.inject.Inject

class MeasurementControlUseCase @Inject constructor(
    private val pairedRecordingCoordinator: PairedRecordingCoordinator,
    private val localController: PhoneRecordingController,
    private val clock: EpochClock,
) {
    suspend fun start(request: MeasurementRequest): AppResult<Unit> = pairedRecordingCoordinator.start(request)

    suspend fun stop(): AppResult<Unit> = pairedRecordingCoordinator.stop(WearStopReason.USER_REQUEST)

    fun annotate(text: String, timestampMillis: Long = clock.nowMillis()): AppResult<Unit> =
        localController.annotate(text, timestampMillis)
}
