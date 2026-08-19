package com.motionapps.sensorbox.presentation.main

import android.content.Intent
import com.motionapps.sensorbox.core.error.AppError
import com.motionapps.sensorbox.core.error.AppErrorCode
import com.motionapps.sensorbox.core.error.AppResult
import com.motionapps.sensorbox.core.error.DiagnosticsStore
import com.motionapps.sensorbox.core.testing.FakeAppPreferencesRepository
import com.motionapps.sensorbox.domain.measurement.DocumentStorageGateway
import com.motionapps.sensorbox.domain.measurement.MeasurementRequest
import com.motionapps.sensorbox.domain.measurement.RecordingWorkflowGateway
import com.motionapps.sensorbox.domain.sensors.SensorDescriptor
import com.motionapps.sensorbox.domain.sensors.WearSensorCatalogStore
import com.motionapps.sensorbox.testing.MainDispatcherRule
import com.motionapps.sensorservices.session.MeasurementSessionStore
import com.motionapps.wearoslib.connectivity.FakeWearConnectionRepository
import com.motionapps.wearoslib.connectivity.ObserveWearCapabilityUseCase
import com.motionapps.wearoslib.connectivity.SendWearMessageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

        assertEquals(3, viewModel.state.value.preferences.sensorSamplingPeriod)
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
    fun `Given storage action When chosen Then recording emits only its picker effect`() = runTest {
        val viewModel = recordingViewModel(FakeRecordingWorkflow())
        val effect = async { viewModel.effects.first() }

        viewModel.accept(RecordingIntent.ChooseStorage)

        assertEquals(RecordingEffect.PickStorageDirectory, effect.await())
    }

    private fun recordingViewModel(workflow: RecordingWorkflowGateway): RecordingViewModel {
        val repository = FakeWearConnectionRepository()
        return RecordingViewModel(
            preferencesRepository = FakeAppPreferencesRepository(),
            workflow = workflow,
            sessionStore = MeasurementSessionStore(),
            observeWearCapability = ObserveWearCapabilityUseCase(repository),
            sendWearMessage = SendWearMessageUseCase(repository),
            wearSensorCatalog = WearSensorCatalogStore(),
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

private class FakeRecordingWorkflow : RecordingWorkflowGateway {
    var startResult: AppResult<Unit> = AppResult.success(Unit)

    override fun sensors(): List<SensorDescriptor> = emptyList()

    override fun storagePath(): String? = "fixture"

    override fun hasStorage(): Boolean = true

    override fun persistStorage(resultIntent: Intent?): AppResult<Unit> = AppResult.success(Unit)

    override fun missingPermissions(request: MeasurementRequest, includesHeartRate: Boolean): Set<String> = emptySet()

    override suspend fun start(request: MeasurementRequest): AppResult<Unit> = startResult

    override suspend fun stop(): AppResult<Unit> = AppResult.success(Unit)

    override fun annotate(text: String): AppResult<Unit> = AppResult.success(Unit)
}
