package com.tomasrepcik.sensorbox.measurements.list

import com.tomasrepcik.sensorbox.core.failure.AppError
import com.tomasrepcik.sensorbox.core.failure.AppErrorCode
import com.tomasrepcik.sensorbox.core.failure.AppResult
import com.tomasrepcik.sensorbox.measurements.FakeMeasurementRepository
import com.tomasrepcik.sensorbox.measurements.MeasurementTestFixtures
import com.tomasrepcik.sensorbox.measurements.measurementFailureStore
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given saved measurements When refreshed Then the list screen exposes them`() = runTest {
        // Given
        val repository = FakeMeasurementRepository()
        val viewModel = MeasurementsViewModel(repository, measurementFailureStore())

        // When
        viewModel.accept(MeasurementsIntent.Refresh)
        advanceUntilIdle()

        // Then
        assertEquals(listOf(MeasurementTestFixtures.summary), viewModel.state.value.measurements)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `Given a listed measurement When opened Then its identifier is sent to details`() = runTest {
        // Given
        val viewModel = MeasurementsViewModel(FakeMeasurementRepository(), measurementFailureStore())

        // When
        viewModel.accept(MeasurementsIntent.OpenDetails(MeasurementTestFixtures.summary.id))

        // Then
        assertEquals(
            MeasurementsEffect.OpenDetails(MeasurementTestFixtures.summary.id),
            viewModel.effects.first(),
        )
    }

    @Test
    fun `Given repository failure When refreshed Then loading stops with an error`() = runTest {
        // Given
        val repository = FakeMeasurementRepository().apply {
            measurementsResult = AppResult.failure(
                AppError(AppErrorCode.STORAGE, "Read fixture measurement repository"),
            )
        }
        val viewModel = MeasurementsViewModel(repository, measurementFailureStore())

        // When
        viewModel.accept(MeasurementsIntent.Refresh)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(AppErrorCode.STORAGE, viewModel.state.value.errorCode)
    }
}
