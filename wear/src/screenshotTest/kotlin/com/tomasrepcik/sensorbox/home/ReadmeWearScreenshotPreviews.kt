package com.tomasrepcik.sensorbox.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.android.tools.screenshot.PreviewTest
import com.tomasrepcik.sensorbox.core.preferences.AppPreferences
import com.tomasrepcik.sensorbox.core.preferences.DisplayPreferences
import com.tomasrepcik.sensorbox.core.preferences.RecordingPreferences
import com.tomasrepcik.sensorbox.design.WearSensorBoxTheme
import com.tomasrepcik.sensorbox.recording.session.RecordingSessionState
import com.tomasrepcik.sensorbox.recording.sources.WatchSensorDescriptor

@PreviewTest
@ReadmeWearPreview
@Composable
fun ReadmeWearDashboard() = WearReadmeFrame(WearDashboardState(route = WearRoute.MENU))

@PreviewTest
@ReadmeWearPreview
@Composable
fun ReadmeWearLivePicker() = WearReadmeFrame(
    WearDashboardState(route = WearRoute.LIVE, sensors = readmeWearSensors()),
)

@PreviewTest
@ReadmeWearPreview
@Composable
fun WearRecordSelection() = WearReadmeFrame(
    WearDashboardState(
        route = WearRoute.RECORD,
        sensors = readmeWearSensors(),
        selectedSensorIds = setOf(1),
        includesGps = true,
    ),
)

@PreviewTest
@ReadmeWearPreview
@Composable
fun WearSettings() = WearReadmeFrame(
    WearDashboardState(
        route = WearRoute.SETTINGS,
        preferences = AppPreferences(
            recording = RecordingPreferences(sensorSamplingPeriod = 1),
            display = DisplayPreferences(keepWearDisplayOn = true),
        ),
    ),
)

@PreviewTest
@ReadmeWearPreview
@Composable
fun WearActiveRecording() = WearReadmeFrame(
    WearDashboardState(
        route = WearRoute.ACTIVE,
        activeSession = RecordingSessionState.Running("phone", "walk", 0L, listOf(1, 4), true, 60_000L, 1),
    ),
)

@Composable
private fun WearReadmeFrame(state: WearDashboardState) {
    WearSensorBoxTheme {
        Box(Modifier.fillMaxSize()) {
            WearDashboardScreen(
                state = state,
                accept = {},
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(72.dp)
                    .height(28.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Text("10:09")
            }
        }
    }
}

private fun readmeWearSensors() = listOf(
    readmeWearSensor(1, "Goldfish 3-axis Accelerometer"),
    readmeWearSensor(4, "Goldfish 3-axis Gyroscope"),
    readmeWearSensor(2, "Goldfish 3-axis Magnetic field sensor"),
)

private fun readmeWearSensor(type: Int, name: String) = WatchSensorDescriptor(
    type = type,
    name = name,
    vendor = "Android",
    version = 1,
    stringType = "android.sensor.fixture",
    maximumRange = 20f,
    resolution = 0.01f,
    power = 0.1f,
    minimumDelayMicros = 10_000,
    maximumDelayMicros = 200_000,
    reportingMode = 0,
    isWakeUpSensor = false,
)

@Preview(
    device = README_WEAR_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private annotation class ReadmeWearPreview

private const val README_WEAR_DEVICE = "spec:width=454px,height=454px,dpi=320,isRound=true"
