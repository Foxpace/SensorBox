package com.tomasrepcik.sensorbox.measurements.loading

import com.tomasrepcik.sensorbox.measurements.FakeMeasurementRepository
import com.tomasrepcik.sensorbox.measurements.MeasurementTestFixtures
import com.tomasrepcik.sensorbox.measurements.measurementFailureStore
import com.tomasrepcik.sensorbox.measurements.preview.MeasurementPreviewData
import com.tomasrepcik.sensorbox.measurements.preview.MeasurementPreviewStore
import com.tomasrepcik.sensorbox.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementLoadingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `Given a large file When loading Then progress appears before preview opens`() = runTest {
        // Given
        val repository = FakeMeasurementRepository().apply {
            fileLoadGate = CompletableDeferred()
            fileProgress = listOf(0.21f, 0.42f)
        }
        val previewStore = MeasurementPreviewStore()
        val viewModel = MeasurementLoadingViewModel(repository, measurementFailureStore(), previewStore)
        val previewEffect = async { viewModel.effects.first() }

        // When
        viewModel.accept(
            MeasurementLoadingIntent.Load(
                MeasurementTestFixtures.summary.id,
                MeasurementTestFixtures.sensorFile.id,
            ),
        )
        runCurrent()

        // Then
        assertTrue(viewModel.state.value.isLoading)
        assertEquals(0.42f, viewModel.state.value.progress)
        assertFalse(previewEffect.isCompleted)

        repository.fileLoadGate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(
            MeasurementLoadingEffect.OpenPreview(
                MeasurementTestFixtures.summary.id,
                MeasurementTestFixtures.sensorFile.id,
            ),
            previewEffect.await(),
        )
        assertEquals(
            MeasurementPreviewData(
                MeasurementTestFixtures.sensorFile,
                MeasurementTestFixtures.sensorContent,
                MeasurementTestFixtures.sensorMetadata,
            ),
            previewStore.take(MeasurementTestFixtures.summary.id, MeasurementTestFixtures.sensorFile.id),
        )
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `Given a loading file When leaving the screen Then preview does not open`() = runTest {
        // Given
        val repository = FakeMeasurementRepository().apply { fileLoadGate = CompletableDeferred() }
        val viewModel = MeasurementLoadingViewModel(repository, measurementFailureStore(), MeasurementPreviewStore())
        val previewEffect = async { viewModel.effects.first() }
        viewModel.accept(
            MeasurementLoadingIntent.Load(
                MeasurementTestFixtures.summary.id,
                MeasurementTestFixtures.sensorFile.id,
            ),
        )
        runCurrent()

        // When
        viewModel.accept(MeasurementLoadingIntent.Cancel)
        repository.fileLoadGate?.complete(Unit)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(previewEffect.isCompleted)
        previewEffect.cancel()
    }
}
