package com.tomasrepcik.sensorbox.measurements.preview

import com.tomasrepcik.sensorbox.measurements.FakeMeasurementRepository
import com.tomasrepcik.sensorbox.measurements.MeasurementTestFixtures
import com.tomasrepcik.sensorbox.measurements.measurementFailureStore
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementPreviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given a preview route When opened Then CSV data and metadata are loaded`() = runTest {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.accept(
            MeasurementPreviewIntent.Load(
                MeasurementTestFixtures.summary.id,
                MeasurementTestFixtures.sensorFile.id,
            ),
        )
        advanceUntilIdle()

        // Then
        assertEquals(MeasurementTestFixtures.sensorFile, viewModel.state.value.file)
        assertEquals(MeasurementTestFixtures.sensorContent, viewModel.state.value.content)
        assertEquals(MeasurementTestFixtures.sensorMetadata, viewModel.state.value.sensorMetadata)
    }

    @Test
    fun `Given a large CSV When preview opens Then progress is shown on that screen`() = runTest {
        // Given
        val repository = FakeMeasurementRepository().apply {
            fileLoadGate = CompletableDeferred()
            fileProgress = listOf(0.21f, 0.42f)
        }
        val viewModel = viewModel(repository)

        // When
        viewModel.accept(
            MeasurementPreviewIntent.Load(
                MeasurementTestFixtures.summary.id,
                MeasurementTestFixtures.sensorFile.id,
            ),
        )
        runCurrent()

        // Then
        assertTrue(viewModel.state.value.isLoading)
        assertEquals(0.42f, viewModel.state.value.progress)
        assertEquals(MeasurementTestFixtures.sensorFile, viewModel.state.value.file)
        assertNull(viewModel.state.value.content)

        repository.fileLoadGate?.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(1f, viewModel.state.value.progress)
        assertEquals(MeasurementTestFixtures.sensorContent, viewModel.state.value.content)
    }

    @Test
    fun `Given a chart When zoomed at its center Then preview halves the visible time`() = runTest {
        // Given
        val viewModel = viewModel()
        viewModel.accept(
            MeasurementPreviewIntent.Load(
                MeasurementTestFixtures.summary.id,
                MeasurementTestFixtures.sensorFile.id,
            ),
        )
        advanceUntilIdle()

        // When
        viewModel.accept(MeasurementPreviewIntent.ZoomSensorChartTimeWindow(2f, 0.5f))

        // Then
        assertEquals(SensorChartTimeWindow(0.25f, 0.75f), viewModel.state.value.sensorChartTimeWindow)
    }

    private fun viewModel(
        repository: FakeMeasurementRepository = FakeMeasurementRepository(),
    ): MeasurementPreviewViewModel = MeasurementPreviewViewModel(
        repository = repository,
        appFailures = measurementFailureStore(),
    )
}
