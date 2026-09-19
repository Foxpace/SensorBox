package com.tomasrepcik.sensorbox.navigation

import com.tomasrepcik.sensorbox.about.OpenSourceLicense
import com.tomasrepcik.sensorbox.about.OpenSourceLicenseRepository
import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppFailureStore
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.core.failure.DiagnosticsStore
import com.tomasrepcik.sensorbox.core.preferences.AppThemeMode
import com.tomasrepcik.sensorbox.core.storage.MeasurementSyncLock
import com.tomasrepcik.sensorbox.core.testing.FakeAppPreferencesRepository
import com.tomasrepcik.sensorbox.diagnostics.DiagnosticsShareFile
import com.tomasrepcik.sensorbox.diagnostics.DiagnosticsShareFilePreparer
import com.tomasrepcik.sensorbox.onboarding.OnboardingEffect
import com.tomasrepcik.sensorbox.onboarding.OnboardingIntent
import com.tomasrepcik.sensorbox.onboarding.OnboardingViewModel
import com.tomasrepcik.sensorbox.recording.RecordingControlUseCase
import com.tomasrepcik.sensorbox.recording.RecordingDraft
import com.tomasrepcik.sensorbox.recording.RecordingMessage
import com.tomasrepcik.sensorbox.recording.active.ActiveRecordingEffect
import com.tomasrepcik.sensorbox.recording.active.ActiveRecordingIntent
import com.tomasrepcik.sensorbox.recording.active.ActiveRecordingViewModel
import com.tomasrepcik.sensorbox.recording.active.RecordIntent
import com.tomasrepcik.sensorbox.recording.active.RecordViewModel
import com.tomasrepcik.sensorbox.recording.archive.RecordingArchiveRepository
import com.tomasrepcik.sensorbox.recording.archive.RecordingArchiveSelection
import com.tomasrepcik.sensorbox.recording.preview.DevicePreviewRepository
import com.tomasrepcik.sensorbox.recording.preview.GpsPreviewData
import com.tomasrepcik.sensorbox.recording.preview.SensorPreviewData
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStopReason
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionStore
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetup
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetupEffect
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetupIntent
import com.tomasrepcik.sensorbox.recording.setup.RecordingSetupViewModel
import com.tomasrepcik.sensorbox.recording.sources.AvailableRecordingSources
import com.tomasrepcik.sensorbox.recording.sources.AvailableRecordingSourcesUseCase
import com.tomasrepcik.sensorbox.recording.sources.SensorDescriptor
import com.tomasrepcik.sensorbox.settings.SettingsIntent
import com.tomasrepcik.sensorbox.settings.SettingsViewModel
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given available sensors When selecting all Then the ViewModel publishes their selection`() = runTest {
        // Given
        val sources = FakeAvailableRecordingSources(
            AvailableRecordingSources(
                phoneSensors = listOf(SensorDescriptor(type = 1, name = "Accelerometer", vendor = "Fixture")),
                watchSensors = listOf(SensorDescriptor(type = 4, name = "Gyroscope", vendor = "Fixture")),
                isWatchConnected = true,
            ),
        )
        val viewModel = RecordViewModel(sources, failureStore())
        advanceUntilIdle()

        // When
        viewModel.accept(RecordIntent.ToggleAllSensors)

        // Then
        assertEquals(setOf(1), viewModel.state.value.selectedSensorIds)
        assertEquals(setOf(4), viewModel.state.value.selectedWatchSensorIds)
        assertTrue(viewModel.state.value.includesGps)
        assertTrue(viewModel.state.value.watchIncludesGps)
    }

    @Test
    fun `Given onboarding recording archive When completed Then state and navigation effect are owned by onboarding`() =
        runTest {
            val viewModel = OnboardingViewModel(
                preferencesRepository = FakeAppPreferencesRepository(),
                recordingArchive = FakeRecordingArchiveRepository(isSelected = true),
                appFailures = failureStore(),
            )
            val effect = async { viewModel.effects.first() }

            viewModel.accept(OnboardingIntent.AdvanceOnboarding)
            viewModel.accept(OnboardingIntent.CompleteOnboarding)
            advanceUntilIdle()

            assertEquals(1, viewModel.state.value.page)
            assertEquals(OnboardingEffect.Navigate(MainRoute.RECORD), effect.await())
        }

    @Test
    fun `Given onboarding has no recording archive When completed Then recording archive error code is exposed`() =
        runTest {
            val viewModel = OnboardingViewModel(
                preferencesRepository = FakeAppPreferencesRepository(),
                recordingArchive = FakeRecordingArchiveRepository(isSelected = false),
                appFailures = failureStore(),
            )

            viewModel.accept(OnboardingIntent.CompleteOnboarding)

            assertEquals(AppErrorCode.STORAGE, viewModel.state.value.errorCode)
        }

    @Test
    fun `Given archive lookup fails When onboarding completes Then app failure is visible`() = runTest {
        // Given
        val appFailures = failureStore()
        val archive = FakeRecordingArchiveRepository(
            selectedResult = AppResult.failure(AppError(AppErrorCode.STORAGE, "Check fixture archive")),
        )
        val viewModel = OnboardingViewModel(
            FakeAppPreferencesRepository(),
            archive,
            appFailures,
        )

        // When
        viewModel.accept(OnboardingIntent.CompleteOnboarding)

        // Then
        assertEquals(AppErrorCode.STORAGE, appFailures.visibleFailure.value?.code)
        assertEquals(null, viewModel.state.value.errorCode)
    }

    @Test
    fun `Given settings intent When sampling changes Then settings owns updated preference state`() = runTest {
        val viewModel = SettingsViewModel(
            preferencesRepository = FakeAppPreferencesRepository(),
            diagnosticsStore = FakeDiagnosticsStore(),
            ioDispatcher = mainDispatcherRule.dispatcher,
            appFailures = failureStore(),
            devicePreview = FakeDevicePreviewRepository(),
            licenses = FakeOpenSourceLicenseRepository(),
            diagnosticsShareFile = FakeDiagnosticsShareFilePreparer(),
        )
        advanceUntilIdle()

        viewModel.accept(SettingsIntent.SetSamplingPeriod(3))
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.preferences.recording.sensorSamplingPeriod)
    }

    @Test
    fun `Given settings intent When theme changes Then settings owns updated appearance state`() = runTest {
        val viewModel = SettingsViewModel(
            preferencesRepository = FakeAppPreferencesRepository(),
            diagnosticsStore = FakeDiagnosticsStore(),
            ioDispatcher = mainDispatcherRule.dispatcher,
            appFailures = failureStore(),
            devicePreview = FakeDevicePreviewRepository(),
            licenses = FakeOpenSourceLicenseRepository(),
            diagnosticsShareFile = FakeDiagnosticsShareFilePreparer(),
        )
        advanceUntilIdle()

        viewModel.accept(SettingsIntent.SetThemeMode(AppThemeMode.DARK))
        advanceUntilIdle()

        assertEquals(AppThemeMode.DARK, viewModel.state.value.preferences.display.themeMode)
    }

    @Test
    fun `Given diagnostics read failure When viewed Then app failure is visible`() = runTest {
        val diagnostics = FakeDiagnosticsStore(
            readResult = AppResult.failure(AppError(AppErrorCode.STORAGE, "Read fixture diagnostics")),
        )
        val appFailures = failureStore()
        val viewModel = SettingsViewModel(
            FakeAppPreferencesRepository(),
            diagnostics,
            mainDispatcherRule.dispatcher,
            appFailures,
            FakeDevicePreviewRepository(),
            FakeOpenSourceLicenseRepository(),
            FakeDiagnosticsShareFilePreparer(),
        )

        viewModel.accept(SettingsIntent.ViewDiagnostics)
        advanceUntilIdle()

        assertEquals(AppErrorCode.STORAGE, appFailures.visibleFailure.value?.code)
    }

    @Test
    fun `Given no selected source When recording starts Then recording exposes validation code`() = runTest {
        val viewModel = recordingSetupViewModel(FakeRecordingWorkflow())
        advanceUntilIdle()

        viewModel.accept(RecordingSetupIntent.StartRecording)

        assertEquals(RecordingMessage.PICK_AT_LEAST_ONE_SOURCE, viewModel.state.value.message)
        assertEquals(AppErrorCode.VALIDATION, viewModel.state.value.errorCode)
    }

    @Test
    fun `Given archive lookup fails When recording starts Then app failure is visible`() = runTest {
        // Given
        val appFailures = failureStore()
        val archive = FakeRecordingArchiveRepository(
            selectedResult = AppResult.failure(AppError(AppErrorCode.STORAGE, "Check fixture archive")),
        )
        val viewModel = recordingSetupViewModel(
            workflow = FakeRecordingWorkflow(),
            archive = archive,
            appFailures = appFailures,
        )
        advanceUntilIdle()
        viewModel.accept(RecordingSetupIntent.LoadDraft(RecordingDraft(selectedSensorIds = setOf(1))))

        // When
        viewModel.accept(RecordingSetupIntent.StartRecording)

        // Then
        assertEquals(AppErrorCode.STORAGE, appFailures.visibleFailure.value?.code)
        assertEquals(RecordingMessage.NONE, viewModel.state.value.message)
    }

    @Test
    fun `Given archive selected after recording state was created When refreshed Then recording uses its path`() =
        runTest {
            // Given
            val archive = FakeRecordingArchiveRepository(isSelected = false)
            val viewModel = recordingSetupViewModel(
                workflow = FakeRecordingWorkflow(),
                archive = archive,
            )
            assertEquals(null, viewModel.state.value.recordingArchivePath)
            archive.select(RecordingArchiveSelection.Selected("fixture", 0))

            // When
            viewModel.refreshRecordingArchive()

            // Then
            assertEquals("fixture", viewModel.state.value.recordingArchivePath)
        }

    @Test
    fun `Given watch permission rejection When recording starts Then watch guidance is shown`() = runTest {
        val workflow = FakeRecordingWorkflow().apply {
            startResult = AppResult.failure(
                AppError(
                    code = AppErrorCode.PERMISSION,
                    operation = "Handle paired PREPARE acknowledgement",
                    diagnosticMessage = "Paired PREPARE permission rejected",
                    context = mapOf("source" to "watch"),
                ),
            )
        }
        val viewModel = recordingSetupViewModel(workflow)
        advanceUntilIdle()

        viewModel.accept(RecordingSetupIntent.LoadDraft(RecordingDraft(selectedWatchSensorIds = setOf(1))))
        viewModel.accept(RecordingSetupIntent.StartRecording)
        advanceUntilIdle()

        assertEquals(RecordingMessage.WATCH_PERMISSION_REQUIRED, viewModel.state.value.message)
        assertEquals(AppErrorCode.PERMISSION, viewModel.state.value.errorCode)
    }

    @Test
    fun `Given waiting start When tapped Then progress appears and duplicate taps are ignored`() = runTest {
        val startGate = CompletableDeferred<Unit>()
        val workflow = FakeRecordingWorkflow().apply { this.startGate = startGate }
        val sessionStore = RecordingSessionStore()
        val viewModel = recordingSetupViewModel(workflow, sessionStore = sessionStore)
        advanceUntilIdle()

        viewModel.accept(RecordingSetupIntent.LoadDraft(RecordingDraft(selectedSensorIds = setOf(1))))
        viewModel.accept(RecordingSetupIntent.StartRecording)
        runCurrent()

        assertTrue(viewModel.state.value.isStarting)
        viewModel.accept(RecordingSetupIntent.StartRecording)
        assertEquals(1, workflow.startCalls)

        startGate.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isStarting)
        sessionStore.markRunning(
            RecordingSessionState.Running("session", "fixture", 0L, listOf(1), false),
        )
        runCurrent()

        assertFalse(viewModel.state.value.isStarting)
        sessionStore.markIdle()
        runCurrent()
    }

    @Test
    fun `Given source start failure When recording stops before running Then loading stops`() = runTest {
        // Given
        val sessionStore = RecordingSessionStore()
        val appFailures = failureStore()
        val viewModel = recordingSetupViewModel(
            workflow = FakeRecordingWorkflow(),
            sessionStore = sessionStore,
            appFailures = appFailures,
        )
        advanceUntilIdle()
        viewModel.accept(RecordingSetupIntent.LoadDraft(RecordingDraft(selectedSensorIds = setOf(1))))
        viewModel.accept(RecordingSetupIntent.StartRecording)
        runCurrent()
        assertTrue(viewModel.state.value.isStarting)

        // When
        sessionStore.publishStopped(
            sessionId = "failed-session",
            reason = RecordingSessionStopReason.SOURCE_FAILURE,
            result = AppResult.failure(AppError(AppErrorCode.RECORDING, "Start sensors")),
        )
        runCurrent()

        // Then
        assertFalse(viewModel.state.value.isStarting)
        assertEquals(AppErrorCode.RECORDING, appFailures.visibleFailure.value?.code)
    }

    @Test
    fun `Given a start delay When recording starts Then ViewModel counts down before execution`() = runTest {
        // Given
        val recording = FakeRecordingWorkflow()
        val viewModel = recordingSetupViewModel(recording)
        advanceUntilIdle()
        viewModel.accept(RecordingSetupIntent.LoadDraft(RecordingDraft(selectedSensorIds = setOf(1))))
        viewModel.accept(RecordingSetupIntent.SetStartDelay(2))

        // When
        viewModel.accept(RecordingSetupIntent.StartRecording)
        runCurrent()

        // Then
        assertEquals(2, viewModel.state.value.startCountdownSeconds)
        assertEquals(0, recording.startCalls)
        advanceTimeBy(1_000L.milliseconds)
        runCurrent()
        assertEquals(1, viewModel.state.value.startCountdownSeconds)
        assertEquals(0, recording.startCalls)
        advanceTimeBy(1_000L.milliseconds)
        runCurrent()
        assertEquals(null, viewModel.state.value.startCountdownSeconds)
        assertEquals(1, recording.startCalls)
    }

    @Test
    fun `Given recording archive intent When chosen Then recording emits only its picker effect`() = runTest {
        val viewModel = recordingSetupViewModel(FakeRecordingWorkflow())
        val effect = async { viewModel.effects.first() }

        viewModel.accept(RecordingSetupIntent.ChooseRecordingArchive)

        assertEquals(RecordingSetupEffect.PickRecordingArchive, effect.await())
    }

    @Test
    fun `Given an active recording When stopped Then recording navigates to the main screen`() = runTest {
        val viewModel = ActiveRecordingViewModel(
            recording = FakeRecordingWorkflow(),
            sessionStore = RecordingSessionStore(),
            elapsedRealtimeClock = { 10_000L },
            appFailures = failureStore(),
        )
        val effect = async { viewModel.effects.first() }

        viewModel.accept(ActiveRecordingIntent.StopRecording)
        advanceUntilIdle()

        assertEquals(ActiveRecordingEffect.RecordingStopped, effect.await())
    }

    @Test
    fun `Given available watch sources When observed Then every sensor detail is retained`() = runTest {
        val sources = FakeAvailableRecordingSources()
        val viewModel = RecordViewModel(sources, failureStore())
        advanceUntilIdle()

        sources.update(
            AvailableRecordingSources(
                phoneSensors = emptyList(),
                watchSensors = listOf(
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
                        reportingMode = com.tomasrepcik.sensorbox.recording.sources.SensorReportingMode.CONTINUOUS,
                        isWakeUpSensor = true,
                    ),
                ),
                isWatchConnected = true,
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
                reportingMode = com.tomasrepcik.sensorbox.recording.sources.SensorReportingMode.CONTINUOUS,
                isWakeUpSensor = true,
            ),
            viewModel.state.value.watchSensors.single(),
        )
    }

    @Test
    fun `Given active sync When recording starts Then no recording is requested`() = runTest {
        // Given
        val lock = MeasurementSyncLock()
        val recording = FakeRecordingWorkflow()
        val viewModel = recordingSetupViewModel(recording, syncLock = lock)
        viewModel.accept(RecordingSetupIntent.LoadDraft(RecordingDraft(selectedSensorIds = setOf(1))))
        lock.begin("request")
        runCurrent()

        // When
        viewModel.accept(RecordingSetupIntent.StartRecording)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.state.value.isSyncingWatch)
        assertEquals(0, recording.startCalls)
    }

    @Test
    fun `Given a countdown When sync starts before it ends Then recording is not started`() = runTest {
        // Given
        val lock = MeasurementSyncLock()
        val recording = FakeRecordingWorkflow()
        val viewModel = recordingSetupViewModel(recording, syncLock = lock)
        viewModel.accept(RecordingSetupIntent.LoadDraft(RecordingDraft(selectedSensorIds = setOf(1))))
        viewModel.accept(RecordingSetupIntent.SetStartDelay(2))
        viewModel.accept(RecordingSetupIntent.StartRecording)
        runCurrent()

        // When
        lock.begin("request")
        advanceUntilIdle()

        // Then
        assertEquals(0, recording.startCalls)
        assertFalse(viewModel.state.value.isStarting)
    }

    @Test
    fun `Given an open folder picker When sync starts before selection returns Then the archive is unchanged`() =
        runTest {
            // Given
            val lock = MeasurementSyncLock()
            val archive = FakeRecordingArchiveRepository(isSelected = false)
            val viewModel = recordingSetupViewModel(FakeRecordingWorkflow(), archive = archive, syncLock = lock)
            viewModel.accept(RecordingSetupIntent.ChooseRecordingArchive)
            lock.begin("request")

            // When
            viewModel.handleRecordingArchiveResult(RecordingArchiveSelection.Selected("content://new", 3))

            // Then
            assertFalse(archive.isSelected().getOrThrow())
        }

    private fun recordingSetupViewModel(
        workflow: RecordingControlUseCase,
        sessionStore: RecordingSessionStore = RecordingSessionStore(),
        archive: RecordingArchiveRepository = FakeRecordingArchiveRepository(isSelected = true),
        appFailures: AppFailureStore = failureStore(),
        syncLock: MeasurementSyncLock = MeasurementSyncLock(),
    ): RecordingSetupViewModel = RecordingSetupViewModel(
        preferencesRepository = FakeAppPreferencesRepository(),
        recordingArchive = archive,
        permissions = { emptySet() },
        recording = workflow,
        sessionStore = sessionStore,
        appFailures = appFailures,
        syncLock = syncLock,
    )

    private fun failureStore() = AppFailureStore { }
}

private class FakeAvailableRecordingSources(
    initial: AvailableRecordingSources = AvailableRecordingSources(phoneSensors = emptyList()),
) : AvailableRecordingSourcesUseCase {
    private val sources = MutableStateFlow(initial)

    override val current: AvailableRecordingSources
        get() = sources.value

    override fun observe(): Flow<AvailableRecordingSources> = sources

    fun update(value: AvailableRecordingSources) {
        sources.value = value
    }
}

private class FakeRecordingArchiveRepository(
    isSelected: Boolean = true,
    private val selectedResult: AppResult<Boolean>? = null,
) : RecordingArchiveRepository {
    private var selected = isSelected

    override fun isSelected(): AppResult<Boolean> = selectedResult ?: AppResult.success(selected)

    override fun path(): AppResult<String?> = isSelected().fold(
        onSuccess = { AppResult.success(if (selected) "fixture" else null) },
        onFailure = { AppResult.success(null) },
    )

    override fun select(selection: RecordingArchiveSelection.Selected): AppResult<Unit> {
        selected = true
        return AppResult.success(Unit)
    }
}

private class FakeDiagnosticsStore(
    private val readResult: AppResult<String> = AppResult.success("fixture diagnostics"),
) : DiagnosticsStore {
    override fun readText(): AppResult<String> = readResult

    override fun clear(): AppResult<Unit> = AppResult.success(Unit)
}

private class FakeDevicePreviewRepository : DevicePreviewRepository {
    override fun observeGps(intervalSeconds: Int, minimumDistanceMeters: Int) =
        kotlinx.coroutines.flow.flowOf(AppResult.success(GpsPreviewData(hasPermission = true)))

    override fun observeSensor(sensorType: Int) =
        kotlinx.coroutines.flow.flowOf(AppResult.success(SensorPreviewData(isAvailable = true)))

    override fun batteryOptimizationExemption(): AppResult<Boolean> = AppResult.success(false)
}

private class FakeOpenSourceLicenseRepository : OpenSourceLicenseRepository {
    override fun load(): AppResult<List<OpenSourceLicense>> = AppResult.success(emptyList())
}

private class FakeDiagnosticsShareFilePreparer : DiagnosticsShareFilePreparer {
    override fun prepare(text: String): AppResult<DiagnosticsShareFile> = AppResult.success(
        DiagnosticsShareFile("content://diagnostics", "diagnostics.jsonl", "application/x-ndjson"),
    )
}

private class FakeRecordingWorkflow : RecordingControlUseCase {
    var startResult: AppResult<Unit> = AppResult.success(Unit)
    var startGate: CompletableDeferred<Unit>? = null
    var startCalls = 0

    override suspend fun start(request: RecordingSetup): AppResult<Unit> {
        startCalls += 1
        startGate?.await()
        return startResult
    }

    override suspend fun stop(): AppResult<Unit> = AppResult.success(Unit)

    override fun annotate(text: String): AppResult<Unit> = AppResult.success(Unit)
}
