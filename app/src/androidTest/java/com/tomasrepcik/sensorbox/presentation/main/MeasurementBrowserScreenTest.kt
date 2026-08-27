package com.tomasrepcik.sensorbox.presentation.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.tomasrepcik.sensorbox.domain.measurements.GpsCoordinate
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementDetails
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileContent
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileKind
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementFileSummary
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementMetadataEntry
import com.tomasrepcik.sensorbox.domain.measurements.MeasurementSummary
import com.tomasrepcik.sensorbox.domain.measurements.SensorSeriesSample
import com.tomasrepcik.sensorbox.ui.theme.SensorBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MeasurementBrowserScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenArchiveWhenMeasurementIsTappedThenItsIdentifierIsSelected() {
        var selectedId: String? = null
        composeRule.setContent {
            SensorBoxTheme {
                MeasurementsScreen(
                    state = MeasurementBrowserState(measurements = listOf(summary)),
                    onIntent = { intent ->
                        if (intent is MeasurementBrowserIntent.OpenMeasurementDetails) {
                            selectedId = intent.measurementId
                        }
                    },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Morning walk").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(summary.id, selectedId) }
    }

    @Test
    fun givenMeasurementDetailsThenAllMetadataAndFilesCanBeInspected() {
        var selectedFile: String? = null
        composeRule.setContent {
            SensorBoxTheme {
                MeasurementDetailsScreen(
                    state = MeasurementBrowserState(selectedMeasurement = details),
                    onIntent = { intent ->
                        if (intent is MeasurementBrowserIntent.OpenMeasurementFile) selectedFile = intent.fileId
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
    fun givenSensorSamplesThenTheirSeriesAndChartSummaryAreShown() {
        var chartIntent: MeasurementBrowserIntent? = null
        showFile(
            sensorFile,
            MeasurementFileContent.SensorSeries(
                columns = listOf("x", "y", "z"),
                samples = listOf(
                    SensorSeriesSample(1, listOf(0.1, 0.2, 0.3)),
                    SensorSeriesSample(2, listOf(0.4, 0.5, 0.6)),
                ),
                truncated = false,
            ),
            onIntent = { chartIntent = it },
        )

        composeRule.onNodeWithText("2 chart samples").assertIsDisplayed()
        composeRule.onNodeWithText("x, y, z").assertIsDisplayed()
        composeRule.onNodeWithText("Zoom out").assertIsDisplayed()
        composeRule.onNodeWithText("Show all").assertIsDisplayed()
        composeRule.onNodeWithText("Zoom in").assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(MeasurementBrowserIntent.ZoomSensorChartTimeWindow(2f, 0.5f), chartIntent)
        }
    }

    @Test
    fun givenGpsSamplesThenCoordinatesKeepNavigationPrecision() {
        showFile(
            gpsFile,
            MeasurementFileContent.GpsCoordinates(
                listOf(GpsCoordinate(1_725_000_000_000, 48.148596, 17.107748, 140.0, 3.0, null, null, "gps")),
                truncated = false,
            ),
        )

        composeRule.onNodeWithText("Coordinate 1").assertIsDisplayed()
        composeRule.onNodeWithText("48.148596 , 17.107748").assertIsDisplayed()
    }

    private fun showFile(
        file: MeasurementFileSummary,
        content: MeasurementFileContent,
        onIntent: (MeasurementBrowserIntent) -> Unit = {},
    ) {
        composeRule.setContent {
            SensorBoxTheme {
                MeasurementFileScreen(
                    state = MeasurementBrowserState(selectedFile = file, selectedFileContent = content),
                    onIntent = onIntent,
                    onBack = {},
                )
            }
        }
    }

    private companion object {
        val summary = MeasurementSummary("session-1", "Morning walk", 1_725_000_000_000, "2026-08-25", 2)
        val sensorFile = MeasurementFileSummary("accelerometer.csv", "Accelerometer", MeasurementFileKind.SENSOR, 256)
        val gpsFile = MeasurementFileSummary("gps.csv", "Gps", MeasurementFileKind.GPS, 128)
        val details = MeasurementDetails(
            summary,
            listOf(MeasurementMetadataEntry("device.model", "Pixel fixture")),
            listOf(sensorFile, gpsFile),
        )
    }
}
