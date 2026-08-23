package com.motionapps.sensorbox.domain.measurement

import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.time.EpochClock
import com.motionapps.sensorbox.domain.paired.PairedRecordingCoordinator
import com.motionapps.wearoslib.protocol.WearStopReason
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
