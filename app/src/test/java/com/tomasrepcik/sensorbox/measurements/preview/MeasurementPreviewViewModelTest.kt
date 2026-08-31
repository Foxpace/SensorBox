package com.tomasrepcik.sensorbox.measurements.preview

import com.tomasrepcik.sensorbox.measurements.FakeMeasurementRepository
import com.tomasrepcik.sensorbox.measurements.MeasurementTestFixtures
import com.tomasrepcik.sensorbox.measurements.measurementFailureStore
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementPreviewViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given loaded sensor content When shown Then preview owns the chart data`() {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.accept(
            MeasurementPreviewIntent.Show(
                MeasurementTestFixtures.sensorFile,
                MeasurementTestFixtures.sensorContent,
                MeasurementTestFixtures.sensorMetadata,
            ),
        )

        // Then
        assertEquals(MeasurementTestFixtures.sensorFile, viewModel.state.value.file)
        assertEquals(MeasurementTestFixtures.sensorContent, viewModel.state.value.content)
        assertEquals(MeasurementTestFixtures.sensorMetadata, viewModel.state.value.sensorMetadata)
    }

    @Test
    fun `Given loading completed When preview opens Then cached chart data is shown`() {
        // Given
        val previewStore = MeasurementPreviewStore().apply {
            put(
                MeasurementTestFixtures.summary.id,
                MeasurementTestFixtures.sensorFile.id,
                MeasurementPreviewData(
                    MeasurementTestFixtures.sensorFile,
                    MeasurementTestFixtures.sensorContent,
                    MeasurementTestFixtures.sensorMetadata,
                ),
            )
        }
        val viewModel = viewModel(previewStore)

        // When
        viewModel.accept(
            MeasurementPreviewIntent.Load(
                MeasurementTestFixtures.summary.id,
                MeasurementTestFixtures.sensorFile.id,
            ),
        )

        // Then
        assertEquals(MeasurementTestFixtures.sensorContent, viewModel.state.value.content)
    }

    @Test
    fun `Given restored preview route When cache is empty Then chart data is loaded again`() = runTest {
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
        assertEquals(MeasurementTestFixtures.sensorContent, viewModel.state.value.content)
    }

    @Test
    fun `Given a chart When zoomed at its center Then preview halves the visible time`() {
        // Given
        val viewModel = viewModel()

        // When
        viewModel.accept(MeasurementPreviewIntent.ZoomSensorChartTimeWindow(2f, 0.5f))

        // Then
        assertEquals(SensorChartTimeWindow(0.25f, 0.75f), viewModel.state.value.sensorChartTimeWindow)
    }

    private fun viewModel(
        previewStore: MeasurementPreviewStore = MeasurementPreviewStore(),
    ): MeasurementPreviewViewModel = MeasurementPreviewViewModel(
        repository = FakeMeasurementRepository(),
        appFailures = measurementFailureStore(),
        previewStore = previewStore,
    )
}
