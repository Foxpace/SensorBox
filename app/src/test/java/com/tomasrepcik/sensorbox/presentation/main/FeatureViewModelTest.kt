package com.tomasrepcik.sensorbox.presentation.main

import android.content.Intent
import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticsStore
import com.tomasrepcik.sensorbox.core.preferences.AppThemeMode
import com.tomasrepcik.sensorbox.core.testing.FakeAppPreferencesRepository
import com.tomasrepcik.sensorbox.domain.measurement.DocumentStorageGateway
import com.tomasrepcik.sensorbox.domain.measurement.MeasurementPermissionsUseCase
import com.tomasrepcik.sensorbox.domain.measurement.MeasurementRequest
import com.tomasrepcik.sensorbox.domain.measurement.RecordingControlUseCase
import com.tomasrepcik.sensorbox.domain.sensors.AvailableSensorsUseCase
import com.tomasrepcik.sensorbox.domain.sensors.SensorDescriptor
import com.tomasrepcik.sensorbox.domain.sensors.WearSensorCatalogStore
import com.tomasrepcik.sensorbox.sensorservices.session.MeasurementSessionState
import com.tomasrepcik.sensorbox.sensorservices.session.MeasurementSessionStore
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import com.tomasrepcik.sensorbox.wearoslib.connectivity.FakeWearConnectionRepository
import com.tomasrepcik.sensorbox.wearoslib.connectivity.ObserveWearCapabilityUseCase
import com.tomasrepcik.sensorbox.wearoslib.connectivity.SendWearMessageUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.SendWearCommandUseCase
import com.tomasrepcik.sensorbox.wearoslib.protocol.WearSensorInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given onboarding storage When completed Then state and navigation effect are owned by onboarding`() = runTest {
        val viewModel = OnboardingViewModel(
            preferencesRepository = FakeAppPreferencesRepository(),
            documentStorage = FakeDocumentStorage(hasStorage = true),
        )
        val effect = async { viewModel.effects.first() }

        viewModel.accept(OnboardingIntent.AdvanceOnboarding)
        viewModel.accept(OnboardingIntent.CompleteOnboarding)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.page)
        assertEquals(OnboardingEffect.Navigate(MainRoute.RECORD), effect.await())
    }

    @Test
    fun `Given onboarding has no storage When completed Then storage error code is exposed`() = runTest {
        val viewModel = OnboardingViewModel(
            preferencesRepository = FakeAppPreferencesRepository(),
            documentStorage = FakeDocumentStorage(hasStorage = false),
        )

        viewModel.accept(OnboardingIntent.CompleteOnboarding)

        assertEquals(AppErrorCode.STORAGE, viewModel.state.value.errorCode)
    }

    @Test
    fun `Given settings action When sampling changes Then settings owns updated preference state`() = runTest {
        val viewModel = SettingsViewModel(
            preferencesRepository = FakeAppPreferencesRepository(),
            diagnosticsStore = FakeDiagnosticsStore(),
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
        advanceUntilIdle()

        viewModel.accept(SettingsIntent.SetSamplingPeriod(3))
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.preferences.recording.sensorSamplingPeriod)
    }

    @Test
    fun `Given settings action When theme changes Then settings owns updated appearance state`() = runTest {
        val viewModel = SettingsViewModel(
            preferencesRepository = FakeAppPreferencesRepository(),
            diagnosticsStore = FakeDiagnosticsStore(),
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
        advanceUntilIdle()

        viewModel.accept(SettingsIntent.SetThemeMode(AppThemeMode.DARK))
        advanceUntilIdle()

        assertEquals(AppThemeMode.DARK, viewModel.state.value.preferences.display.themeMode)
    }

    @Test
    fun `Given diagnostics read failure When viewed Then settings emits the stable error code`() = runTest {
        val diagnostics = FakeDiagnosticsStore(
            readResult = AppResult.failure(AppError(AppErrorCode.STORAGE, "Read fixture diagnostics")),
        )
        val viewModel = SettingsViewModel(
            FakeAppPreferencesRepository(),
            diagnostics,
            mainDispatcherRule.dispatcher,
        )
        val effect = async { viewModel.effects.first() }

        viewModel.accept(SettingsIntent.ViewDiagnostics)
        advanceUntilIdle()

        assertEquals(AppErrorCode.STORAGE, viewModel.state.value.errorCode)
        assertEquals(SettingsEffect.DiagnosticsFailed(AppErrorCode.STORAGE), effect.await())
    }

    @Test
    fun `Given no selected source When recording starts Then recording exposes validation code`() = runTest {
        val viewModel = recordingViewModel(FakeRecordingWorkflow())
        advanceUntilIdle()

        viewModel.accept(RecordingIntent.StartMeasurement)

        assertEquals(RecordingMessage.PICK_AT_LEAST_ONE_SOURCE, viewModel.state.value.message)
        assertEquals(AppErrorCode.VALIDATION, viewModel.state.value.errorCode)
    }

    @Test
    fun `Given Wear permission rejection When recording starts Then Wear guidance is shown`() = runTest {
        val workflow = FakeRecordingWorkflow().apply {
            startResult = AppResult.failure(
                AppError(
                    code = AppErrorCode.PERMISSION,
                    operation = "Handle paired PREPARE acknowledgement",
                    diagnosticMessage = "Paired PREPARE permission rejected",
                    context = mapOf("source" to "wear"),
                ),
            )
        }
        val viewModel = recordingViewModel(workflow)
        advanceUntilIdle()

        viewModel.accept(RecordingIntent.ToggleWearSensor(1))
        viewModel.accept(RecordingIntent.StartMeasurement)
        advanceUntilIdle()

        assertEquals(RecordingMessage.WEAR_PERMISSION_REQUIRED, viewModel.state.value.message)
        assertEquals(AppErrorCode.PERMISSION, viewModel.state.value.errorCode)
    }

    @Test
    fun `Given waiting start When tapped Then progress appears and duplicate taps are ignored`() = runTest {
        val startGate = CompletableDeferred<Unit>()
        val workflow = FakeRecordingWorkflow().apply { this.startGate = startGate }
        val sessionStore = MeasurementSessionStore()
        val viewModel = recordingViewModel(workflow, sessionStore = sessionStore)
        advanceUntilIdle()

        viewModel.accept(RecordingIntent.ToggleSensor(1))
        viewModel.accept(RecordingIntent.StartMeasurement)
        runCurrent()

        assertTrue(viewModel.state.value.isStarting)
        viewModel.accept(RecordingIntent.StartMeasurement)
        assertEquals(1, workflow.startCalls)

        startGate.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isStarting)
        sessionStore.markRunning(
            MeasurementSessionState.Running("session", "fixture", 0L, listOf(1), false),
        )
        runCurrent()

        assertFalse(viewModel.state.value.isStarting)
        sessionStore.markIdle()
        runCurrent()
    }

    @Test
    fun `Given a start delay When recording starts Then ViewModel counts down before execution`() = runTest {
        // Given
        val recording = FakeRecordingWorkflow()
        val viewModel = recordingViewModel(recording)
        advanceUntilIdle()
        viewModel.accept(RecordingIntent.ToggleSensor(1))
        viewModel.accept(RecordingIntent.SetStartDelay(2))

        // When
        viewModel.accept(RecordingIntent.StartMeasurement)
        runCurrent()

        // Then
        assertEquals(2, viewModel.state.value.startCountdownSeconds)
        assertEquals(0, recording.startCalls)
        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(1, viewModel.state.value.startCountdownSeconds)
        assertEquals(0, recording.startCalls)
        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(null, viewModel.state.value.startCountdownSeconds)
        assertEquals(1, recording.startCalls)
    }

    @Test
    fun `Given storage action When chosen Then recording emits only its picker effect`() = runTest {
        val viewModel = recordingViewModel(FakeRecordingWorkflow())
        val effect = async { viewModel.effects.first() }

        viewModel.accept(RecordingIntent.ChooseStorage)

        assertEquals(RecordingEffect.PickStorageDirectory, effect.await())
    }

    @Test
    fun `Given an active measurement When stopped Then recording navigates to the main screen`() = runTest {
        val viewModel = recordingViewModel(FakeRecordingWorkflow())
        val effect = async { viewModel.effects.first() }

        viewModel.accept(RecordingIntent.StopMeasurement)
        advanceUntilIdle()

        assertEquals(RecordingEffect.Navigate(MainRoute.RECORD), effect.await())
    }

    @Test
    fun `Given a Wear catalog When observed Then every sensor detail is retained`() = runTest {
        val catalog = WearSensorCatalogStore()
        val viewModel = recordingViewModel(FakeRecordingWorkflow(), catalog)
        advanceUntilIdle()

        catalog.update(
            listOf(
                WearSensorInfo(
                    type = 1,
                    name = "Wear Accelerometer",
                    vendor = "Fixture",
                    version = 7,
                    stringType = "android.sensor.accelerometer",
                    maximumRange = 78.4f,
                    resolution = 0.0024f,
                    power = 0.25f,
                    minimumDelayMicros = 5_000,
                    maximumDelayMicros = 200_000,
                    reportingMode = 0,
                    isWakeUpSensor = true,
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            SensorDescriptor(
                type = 1,
                name = "Wear Accelerometer",
                vendor = "Fixture",
                version = 7,
                stringType = "android.sensor.accelerometer",
                maximumRange = 78.4f,
                resolution = 0.0024f,
                power = 0.25f,
                minimumDelayMicros = 5_000,
                maximumDelayMicros = 200_000,
                reportingMode = com.tomasrepcik.sensorbox.domain.sensors.SensorReportingMode.CONTINUOUS,
                isWakeUpSensor = true,
            ),
            viewModel.state.value.wearSensors.single(),
        )
    }

    private fun recordingViewModel(
        workflow: RecordingControlUseCase,
        wearSensorCatalog: WearSensorCatalogStore = WearSensorCatalogStore(),
        sessionStore: MeasurementSessionStore = MeasurementSessionStore(),
    ): RecordingViewModel {
        val repository = FakeWearConnectionRepository()
        return RecordingViewModel(
            preferencesRepository = FakeAppPreferencesRepository(),
            availableSensors = AvailableSensorsUseCase { emptyList() },
            storage = FakeDocumentStorage(hasStorage = true),
            permissions = MeasurementPermissionsUseCase { emptySet() },
            recording = workflow,
            sessionStore = sessionStore,
            observeWearCapability = ObserveWearCapabilityUseCase(repository),
            sendWearCommand = SendWearCommandUseCase(SendWearMessageUseCase(repository)),
            wearSensorCatalog = wearSensorCatalog,
            elapsedRealtimeClock = ElapsedRealtimeClock { 10_000L },
        )
    }
}

private class FakeDocumentStorage(private val hasStorage: Boolean) : DocumentStorageGateway {
    override fun hasStorage(): AppResult<Boolean> = AppResult.success(hasStorage)

    override fun displayPath(): AppResult<String?> = AppResult.success(if (hasStorage) "fixture" else null)

    override fun persist(resultIntent: Intent): AppResult<Unit> = AppResult.success(Unit)
}

private class FakeDiagnosticsStore(
    private val readResult: AppResult<String> = AppResult.success("fixture diagnostics"),
) : DiagnosticsStore {
    override fun readText(): AppResult<String> = readResult

    override fun exportFile(): AppResult<File> = AppResult.failure(
        AppError(AppErrorCode.STORAGE, "Export fixture diagnostics"),
    )

    override fun clear(): AppResult<Unit> = AppResult.success(Unit)
}

private class FakeRecordingWorkflow : RecordingControlUseCase {
    var startResult: AppResult<Unit> = AppResult.success(Unit)
    var startGate: CompletableDeferred<Unit>? = null
    var startCalls = 0

    override suspend fun start(request: MeasurementRequest): AppResult<Unit> {
        startCalls += 1
        startGate?.await()
        return startResult
    }

    override suspend fun stop(): AppResult<Unit> = AppResult.success(Unit)

    override fun annotate(text: String): AppResult<Unit> = AppResult.success(Unit)
}
