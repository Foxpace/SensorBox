package com.tomasrepcik.sensorbox.presentation.main

import com.tomasrepcik.sensorbox.core.error.AppError
import com.tomasrepcik.sensorbox.core.error.AppErrorCode
import com.tomasrepcik.sensorbox.core.error.AppFailureStore
import com.tomasrepcik.sensorbox.core.error.AppResult
import com.tomasrepcik.sensorbox.core.error.DiagnosticLogger
import com.tomasrepcik.sensorbox.domain.measurements.GpsCoordinate
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementDetails
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileContent
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileKind
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileSummary
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementMetadataEntry
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementRepository
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementSummary
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementBrowserViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given saved measurements When refreshed Then newest folders are exposed`() = runTest {
        val repository = FakeMeasurementRepository()
        val viewModel = MeasurementBrowserViewModel(repository, failureStore())

        viewModel.onIntent(MeasurementBrowserIntent.RefreshMeasurements)
        advanceUntilIdle()

        assertEquals(listOf(repository.summary), viewModel.state.value.measurements)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `Given a measurement When its GPS file opens Then details content and navigation are retained`() = runTest {
        val repository = FakeMeasurementRepository()
        val viewModel = MeasurementBrowserViewModel(repository, failureStore())
        val detailsEffect = async { viewModel.effects.first() }

        viewModel.onIntent(MeasurementBrowserIntent.OpenMeasurementDetails(repository.summary.id))
        advanceUntilIdle()

        assertEquals(repository.details, viewModel.state.value.selectedMeasurement)
        assertEquals(
            MeasurementBrowserEffect.Navigate(MainRoute.MEASUREMENT_DETAILS),
            detailsEffect.await(),
        )

        val fileEffect = async { viewModel.effects.first() }
        viewModel.onIntent(MeasurementBrowserIntent.OpenMeasurementFile(repository.gpsFile.id))
        advanceUntilIdle()

        assertEquals(repository.gpsFile, viewModel.state.value.selectedFile)
        assertEquals(repository.gpsContent, viewModel.state.value.selectedFileContent)
        assertEquals(
            MeasurementBrowserEffect.Navigate(MainRoute.MEASUREMENT_FILE),
            fileEffect.await(),
        )
    }

    @Test
    fun `Given repository failure When refreshed Then loading stops and stable error state is exposed`() = runTest {
        val repository = FakeMeasurementRepository().apply {
            measurementsResult = AppResult.failure(
                AppError(AppErrorCode.STORAGE, "Read fixture measurement repository"),
            )
        }
        val viewModel = MeasurementBrowserViewModel(repository, failureStore())

        viewModel.onIntent(MeasurementBrowserIntent.RefreshMeasurements)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isLoading)
        assertEquals(AppErrorCode.STORAGE, viewModel.state.value.errorCode)
    }

    private fun failureStore() = AppFailureStore(DiagnosticLogger { })
}

private class FakeMeasurementRepository : MeasurementRepository {
    val summary = MeasurementSummary("session-1", "Morning walk", 1_725_000_000_000, "2026-08-25", 1)
    val gpsFile = MeasurementFileSummary("gps.csv", "Gps", MeasurementFileKind.GPS, 128)
    val details = MeasurementDetails(
        summary,
        listOf(MeasurementMetadataEntry("device.model", "Pixel fixture")),
        listOf(gpsFile),
    )
    val gpsContent = MeasurementFileContent.GpsCoordinates(
        listOf(GpsCoordinate(1_725_000_000_000, 48.148596, 17.107748, 140.0, 3.0, null, null, "gps")),
        truncated = false,
    )
    var measurementsResult: AppResult<List<MeasurementSummary>> = AppResult.success(listOf(summary))

    override suspend fun loadMeasurements(): AppResult<List<MeasurementSummary>> = measurementsResult

    override suspend fun loadMeasurementDetails(measurementId: String): AppResult<MeasurementDetails> =
        AppResult.success(details)

    override suspend fun loadMeasurementFile(measurementId: String, fileId: String): AppResult<MeasurementFileContent> =
        AppResult.success(gpsContent)
}
