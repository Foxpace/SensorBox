package com.tomasrepcik.sensorbox.measurements.details

import com.tomasrepcik.sensorbox.measurements.FakeMeasurementRepository
import com.tomasrepcik.sensorbox.measurements.MeasurementTestFixtures
import com.tomasrepcik.sensorbox.measurements.measurementFailureStore
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given a measurement identifier When loaded Then details belong to this screen`() = runTest {
        // Given
        val viewModel = MeasurementDetailsViewModel(FakeMeasurementRepository(), measurementFailureStore())

        // When
        viewModel.accept(MeasurementDetailsIntent.Load(MeasurementTestFixtures.summary.id))
        advanceUntilIdle()

        // Then
        assertEquals(MeasurementTestFixtures.details, viewModel.state.value.details)
    }

    @Test
    fun `Given loaded details When a file opens Then loading receives its identifiers`() = runTest {
        // Given
        val viewModel = MeasurementDetailsViewModel(FakeMeasurementRepository(), measurementFailureStore())
        viewModel.accept(MeasurementDetailsIntent.Load(MeasurementTestFixtures.summary.id))
        advanceUntilIdle()

        // When
        viewModel.accept(MeasurementDetailsIntent.OpenFile(MeasurementTestFixtures.sensorFile.id))

        // Then
        assertEquals(
            MeasurementDetailsEffect.OpenFile(
                MeasurementTestFixtures.summary.id,
                MeasurementTestFixtures.sensorFile.id,
            ),
            viewModel.effects.first(),
        )
    }

    @Test
    fun `Given loaded details When the same screen returns Then its state is preserved`() = runTest {
        // Given
        val repository = FakeMeasurementRepository()
        val viewModel = MeasurementDetailsViewModel(repository, measurementFailureStore())
        viewModel.accept(MeasurementDetailsIntent.Load(MeasurementTestFixtures.summary.id))
        advanceUntilIdle()
        val loadedState = viewModel.state.value

        // When
        viewModel.accept(MeasurementDetailsIntent.Load(MeasurementTestFixtures.summary.id))
        advanceUntilIdle()

        // Then
        assertEquals(1, repository.detailsLoadCount)
        assertEquals(loadedState, viewModel.state.value)
    }
}
