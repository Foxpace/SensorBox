package com.tomasrepcik.sensorbox.domain.paired

import com.motionapps.sensorservices.intent.MeasurementLaunchRequest
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import com.motionapps.wearoslib.connectivity.WearConnectionRepository
import com.motionapps.wearoslib.connectivity.WearNode
import com.motionapps.wearoslib.protocol.SendWearCommandUseCase
import com.motionapps.wearoslib.protocol.WearAcknowledgementOutcome
import com.motionapps.wearoslib.protocol.WearCommand
import com.motionapps.wearoslib.protocol.WearCommandCodec
import com.motionapps.wearoslib.protocol.WearSessionCommand
import com.motionapps.wearoslib.protocol.WearStopReason
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.core.time.EpochClock
import com.tomasrepcik.sensorbox.domain.measurement.MeasurementRequest
import com.tomasrepcik.sensorbox.domain.measurement.PhoneRecordingController
import com.tomasrepcik.sensorbox.domain.measurement.PreparedPhoneRecording
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PairedRecordingCoordinatorTest {
    @Test
    fun `Given Wear rejects prepare When start is requested Then local preparation is aborted`() = runTest {
        val fixture = Fixture()
        fixture.repository.onCommand = { command ->
            when (command) {
                is WearCommand.PrepareRecording -> fixture.ack(
                    command.sessionId,
                    WearSessionCommand.PREPARE,
                    WearAcknowledgementOutcome.REJECTED,
                    AppErrorCode.PERMISSION,
                )

                is WearCommand.AbortRecording -> fixture.succeed(command.sessionId, WearSessionCommand.ABORT)

                else -> Unit
            }
        }

        val result = fixture.coordinator.start(pairedRequest())

        assertTrue(result.isFailure)
        assertEquals(1, fixture.local.abortCalls)
        assertTrue(fixture.repository.commands.any { it is WearCommand.AbortRecording })
        assertEquals(0, fixture.local.commitCalls)
    }

    @Test
    fun `Given no prepare ack When start times out Then three sends and both sides abort`() = runTest {
        val fixture = Fixture()
        fixture.repository.onCommand = { command ->
            if (command is WearCommand.AbortRecording) {
                fixture.succeed(command.sessionId, WearSessionCommand.ABORT)
            }
        }

        val result = fixture.coordinator.start(pairedRequest())

        assertEquals(AppErrorCode.TIMEOUT, result.errorOrNull()?.code)
        assertEquals(3, fixture.repository.commands.count { it is WearCommand.PrepareRecording })
        assertEquals(1, fixture.local.abortCalls)
    }

    @Test
    fun `Given commit is rejected When local commit succeeded Then both devices are compensated`() = runTest {
        val fixture = Fixture()
        fixture.repository.onCommand = { command ->
            when (command) {
                is WearCommand.PrepareRecording -> fixture.succeed(command.sessionId, WearSessionCommand.PREPARE)

                is WearCommand.CommitRecording -> fixture.ack(
                    command.sessionId,
                    WearSessionCommand.COMMIT,
                    WearAcknowledgementOutcome.FAILED,
                    AppErrorCode.MEASUREMENT,
                )

                is WearCommand.AbortRecording -> fixture.succeed(command.sessionId, WearSessionCommand.ABORT)

                else -> Unit
            }
        }

        val result = fixture.coordinator.start(pairedRequest())

        assertTrue(result.isFailure)
        assertEquals(1, fixture.local.commitCalls)
        assertEquals(1, fixture.local.abortCalls)
        assertTrue(fixture.repository.commands.any { it is WearCommand.AbortRecording })
    }

    @Test
    fun `Given local stop fails When paired recording stops Then remote stop is still attempted`() = runTest {
        val fixture = Fixture()
        fixture.repository.onCommand = { command ->
            when (command) {
                is WearCommand.PrepareRecording -> fixture.succeed(command.sessionId, WearSessionCommand.PREPARE)
                is WearCommand.CommitRecording -> fixture.succeed(command.sessionId, WearSessionCommand.COMMIT)
                is WearCommand.StopRecording -> fixture.succeed(command.sessionId, WearSessionCommand.STOP)
                else -> Unit
            }
        }
        assertTrue(fixture.coordinator.start(pairedRequest()).isSuccess)
        fixture.local.stopResult = failure("Stop local recording")

        val result = fixture.coordinator.stop()

        assertTrue(result.isFailure)
        assertEquals(1, fixture.local.stopCalls)
        assertTrue(fixture.repository.commands.any { it is WearCommand.StopRecording })
    }

    private fun pairedRequest() = MeasurementRequest(
        sensorIds = setOf(1),
        includesGps = false,
        samplingPeriodIndex = 0,
        stopOnLowBattery = true,
        useWakeLock = true,
        gpsIntervalSeconds = 10,
        gpsMinDistanceMeters = 20,
        wearSensorIds = setOf(21),
    )

    private class Fixture {
        val inbox = WearAcknowledgementInbox()
        val repository = InteractiveRepository()
        val local = FakePhoneRecordingController()
        val coordinator = PairedRecordingCoordinator(
            localController = local,
            sendCommand = SendWearCommandUseCase(SendWearMessageUseCase(repository)),
            acknowledgementInbox = inbox,
            sessionIdFactory = RecordingSessionIdFactory(),
            diagnosticLogger = DiagnosticLogger { },
            clock = EpochClock { 1_000L },
        )

        fun succeed(sessionId: String, command: WearSessionCommand) {
            ack(sessionId, command, WearAcknowledgementOutcome.SUCCEEDED)
        }

        fun ack(
            sessionId: String,
            command: WearSessionCommand,
            outcome: WearAcknowledgementOutcome,
            errorCode: AppErrorCode? = null,
        ) {
            inbox.publish(WearCommand.Acknowledgement(sessionId, command, outcome, errorCode))
        }
    }
}

private class InteractiveRepository : WearConnectionRepository {
    val commands = mutableListOf<WearCommand>()
    var onCommand: (WearCommand) -> Unit = { }

    override fun observeCapability(capability: String): Flow<com.motionapps.wearoslib.connectivity.WearConnection> =
        emptyFlow()

    override suspend fun findNode(capability: String): WearNode? = WearNode("wear", "Wear", isNearby = true)

    override suspend fun sendMessage(capability: String, path: String, payload: ByteArray): AppResult<Unit> {
        val command = WearCommandCodec.decode(payload).getOrThrow()
        commands += command
        onCommand(command)
        return AppResult.success(Unit)
    }
}

private class FakePhoneRecordingController : PhoneRecordingController {
    var commitCalls = 0
    var abortCalls = 0
    var stopCalls = 0
    var stopResult: AppResult<Unit> = AppResult.success(Unit)

    override suspend fun prepare(sessionId: String, request: MeasurementRequest): AppResult<PreparedPhoneRecording> =
        AppResult.success(
            PreparedPhoneRecording(
                sessionId = sessionId,
                launchRequest = MeasurementLaunchRequest(
                    sessionId = sessionId,
                    folderName = "fixture",
                    useInternalStorage = false,
                    sensorIds = request.sensorIds,
                    sensorSamplingPeriod = 0,
                    includesGps = request.includesGps,
                    stopOnLowBattery = request.stopOnLowBattery,
                    useWakeLock = request.useWakeLock,
                    gpsIntervalSeconds = request.gpsIntervalSeconds,
                    gpsMinDistanceMeters = request.gpsMinDistanceMeters,
                ),
            ),
        )

    override fun commit(prepared: PreparedPhoneRecording, startAtEpochMillis: Long): AppResult<Unit> {
        commitCalls += 1
        return AppResult.success(Unit)
    }

    override fun abort(sessionId: String): AppResult<Unit> {
        abortCalls += 1
        return AppResult.success(Unit)
    }

    override fun stop(sessionId: String, reason: WearStopReason): AppResult<Unit> {
        stopCalls += 1
        return stopResult
    }

    override fun stopAny(reason: WearStopReason): AppResult<Unit> = stopResult

    override fun annotate(text: String, timestampMillis: Long): AppResult<Unit> = AppResult.success(Unit)
}

private fun failure(operation: String): AppResult<Unit> = AppResult.failure(
    AppError(AppErrorCode.MEASUREMENT, operation),
)
