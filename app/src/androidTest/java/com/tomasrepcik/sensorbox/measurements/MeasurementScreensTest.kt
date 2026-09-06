package com.tomasrepcik.sensorbox.measurements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.tomasrepcik.sensorbox.design.SensorBoxTheme
import com.tomasrepcik.sensorbox.measurements.details.MeasurementDetailsIntent
import com.tomasrepcik.sensorbox.measurements.details.MeasurementDetailsScreen
import com.tomasrepcik.sensorbox.measurements.details.MeasurementDetailsState
import com.tomasrepcik.sensorbox.measurements.list.MeasurementsIntent
import com.tomasrepcik.sensorbox.measurements.list.MeasurementsScreen
import com.tomasrepcik.sensorbox.measurements.list.MeasurementsState
import com.tomasrepcik.sensorbox.measurements.preview.MeasurementPreviewIntent
import com.tomasrepcik.sensorbox.measurements.preview.MeasurementPreviewScreen
import com.tomasrepcik.sensorbox.measurements.preview.MeasurementPreviewState
import com.tomasrepcik.sensorbox.measurements.storage.GpsCoordinate
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementDetails
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileContent
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileKind
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementFileSummary
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementMetadataEntry
import com.tomasrepcik.sensorbox.measurements.storage.MeasurementSummary
import com.tomasrepcik.sensorbox.measurements.storage.SensorSeriesSample
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MeasurementScreensTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenMeasurementListWhenMeasurementIsTappedThenItsIdentifierIsSelected() {
        var selectedId: String? = null
        composeRule.setContent {
            SensorBoxTheme {
                MeasurementsScreen(
                    state = MeasurementsState(measurements = listOf(summary)),
                    onIntent = { intent ->
                        if (intent is MeasurementsIntent.OpenDetails) selectedId = intent.measurementId
                    },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Morning walk").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(summary.id, selectedId) }
    }

    @Test
    fun givenMeasurementDetailsThenMetadataAndFilesCanBeInspected() {
        var selectedFile: String? = null
        composeRule.setContent {
            SensorBoxTheme {
                MeasurementDetailsScreen(
                    state = MeasurementDetailsState(details = details),
                    onIntent = { intent ->
                        if (intent is MeasurementDetailsIntent.OpenFile) selectedFile = intent.fileId
                    },
                    onBack = {},
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Pixel fixture"))
        composeRule.onNodeWithText("Pixel fixture").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Accelerometer"))
        composeRule.onNodeWithText("Accelerometer").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(sensorFile.id, selectedFile) }
    }

    @Test
    fun givenLargeCsvWhenPreviewIsLoadingThenProgressIsShownOnThePreviewScreen() {
        composeRule.setContent {
            SensorBoxTheme {
                MeasurementPreviewScreen(
                    state = MeasurementPreviewState(
                        file = sensorFile,
                        progress = 0.42f,
                        isLoading = true,
                    ),
                    onIntent = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Accelerometer").assertIsDisplayed()
        composeRule.onNodeWithText("Loading measurement… 42%").assertIsDisplayed()
        composeRule.onNodeWithText("2 chart samples").assertDoesNotExist()
    }

    @Test
    fun givenSensorSamplesThenPreviewShowsTheirSeriesAndChart() {
        var chartIntent: MeasurementPreviewIntent? = null
        showPreview(
            sensorFile,
            MeasurementFileContent.SensorSeries(
                columns = listOf("x", "y", "z"),
                samples = listOf(
                    SensorSeriesSample(1, listOf(0.1, 0.2, 0.3)),
                    SensorSeriesSample(2, listOf(0.4, 0.5, 0.6)),
                ),
                truncated = false,
            ),
            sensorMetadata,
            onIntent = { chartIntent = it },
        )

        composeRule.onNodeWithText("2 chart samples").assertIsDisplayed()
        composeRule.onNodeWithText("Sensor details").assertIsDisplayed()
        composeRule.onNodeWithText("Bosch accelerometer").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Zoom in"))
        composeRule.onNodeWithText("x, y, z").assertIsDisplayed()
        composeRule.onNodeWithText("Zoom in").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(MeasurementPreviewIntent.ZoomSensorChartTimeWindow(2f, 0.5f), chartIntent)
        }
    }

    @Test
    fun givenGpsSamplesThenPreviewKeepsCoordinatePrecision() {
        showPreview(
            gpsFile,
            MeasurementFileContent.GpsCoordinates(
                listOf(GpsCoordinate(1_725_000_000_000, 48.148596, 17.107748, 140.0, 3.0, null, null, "gps")),
                truncated = false,
            ),
        )

        composeRule.onNodeWithText("Coordinate 1").assertIsDisplayed()
        composeRule.onNodeWithText("48.148596 , 17.107748").assertIsDisplayed()
    }

    @Test
    fun givenActivityTransitionsWhenOpenedThenNamedEventsReplaceTheChart() {
        // Given
        val content = MeasurementFileContent.SensorSeries(
            columns = listOf("activity", "enter_exit"),
            samples = listOf(
                SensorSeriesSample(1_725_000_000_000, listOf(7.0, 0.0)),
                SensorSeriesSample(1_725_000_001_000, listOf(7.0, 1.0)),
            ),
            truncated = false,
        )

        // When
        showPreview(sensorFile.copy(id = "activity_transitions.csv", name = "Activity transitions"), content)

        // Then
        composeRule.onNodeWithText("Walking started").assertIsDisplayed()
        composeRule.onNodeWithText("Walking ended").assertIsDisplayed()
        composeRule.onNodeWithText("2 recorded events").assertIsDisplayed()
        composeRule.onNodeWithText("2 chart samples").assertDoesNotExist()
        composeRule.onNodeWithText("Zoom in").assertDoesNotExist()
    }

    @Test
    fun givenDetectedStepsWhenOpenedThenNumberedEventsReplaceTheChart() {
        // Given
        val content = MeasurementFileContent.SensorSeries(
            columns = listOf("step"),
            samples = listOf(SensorSeriesSample(1_725_000_000_000, listOf(1.0))),
            truncated = false,
        )

        // When
        showPreview(sensorFile.copy(id = "step_detector.csv", name = "Step detector"), content)

        // Then
        composeRule.onNodeWithText("Step detected · 1").assertIsDisplayed()
        composeRule.onNodeWithText("Zoom in").assertDoesNotExist()
    }

    @Test
    fun givenActivityUpdatesWhenOpenedThenTheChartHasReadableLegendLabels() {
        // Given
        val content = MeasurementFileContent.SensorSeries(
            columns = listOf("still", "on_foot", "walking", "running", "vehicle", "bike", "unknown", "tilting"),
            samples = listOf(
                SensorSeriesSample(1_725_000_000_000, listOf(20.0, 80.0, 80.0, 0.0, 0.0, 0.0, 0.0, 0.0)),
            ),
            truncated = false,
        )

        // When
        showPreview(sensorFile.copy(id = "activity_updates.csv", name = "Activity updates"), content)

        // Then
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Walking"))
        composeRule.onNodeWithText("Walking").assertIsDisplayed()
        composeRule.onNodeWithText("Still").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Tilting"))
        composeRule.onNodeWithText("Tilting").assertIsDisplayed()
        composeRule.onNodeWithText("Cycling").assertIsDisplayed()
    }

    private fun showPreview(
        file: MeasurementFileSummary,
        content: MeasurementFileContent,
        metadata: List<MeasurementMetadataEntry> = emptyList(),
        onIntent: (MeasurementPreviewIntent) -> Unit = {},
    ) {
        composeRule.setContent {
            SensorBoxTheme {
                MeasurementPreviewScreen(
                    state = MeasurementPreviewState(
                        file = file,
                        content = content,
                        sensorMetadata = metadata,
                    ),
                    onIntent = onIntent,
                    onBack = {},
                )
            }
        }
    }

    private companion object {
        val summary = MeasurementSummary("session-1", "Morning walk", 1_725_000_000_000, "2026-08-25", 2)
        val sensorFile = MeasurementFileSummary("accelerometer.csv", "Accelerometer", MeasurementFileKind.SENSOR, 256)
        val gpsFile = MeasurementFileSummary("GPS.csv", "GPS", MeasurementFileKind.GPS, 128)
        val sensorMetadata = listOf(MeasurementMetadataEntry("sensor", "Bosch accelerometer"))
        val details = MeasurementDetails(
            summary,
            listOf(MeasurementMetadataEntry("device.model", "Pixel fixture")),
            listOf(sensorFile, gpsFile),
            sensorMetadataByFile = mapOf(sensorFile.id to sensorMetadata),
        )
    }
}
