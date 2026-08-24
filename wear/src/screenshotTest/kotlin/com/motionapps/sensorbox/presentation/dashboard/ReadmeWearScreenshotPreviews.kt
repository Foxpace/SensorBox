package com.motionapps.sensorbox.presentation.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.android.tools.screenshot.PreviewTest
import com.motionapps.sensorbox.domain.sensors.WearSensorDescriptor
import com.motionapps.sensorbox.ui.theme.WearSensorBoxTheme
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer

@PreviewTest
@ReadmeWearPreview
@Composable
fun readmeWearDashboard() = WearReadmeFrame(WearDashboardState(route = WearRoute.MENU))

@PreviewTest
@ReadmeWearPreview
@Composable
fun readmeWearLivePicker() = WearReadmeFrame(
    WearDashboardState(route = WearRoute.LIVE, sensors = readmeWearSensors()),
)

@Composable
private fun WearReadmeFrame(state: WearDashboardState) {
    WearSensorBoxTheme {
        Box(Modifier.fillMaxSize()) {
            WearDashboardScreen(
                state = state,
                chartModelProducer = remember { CartesianChartModelProducer() },
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
    WearSensorDescriptor(1, "Goldfish 3-axis Accelerometer", "Android"),
    WearSensorDescriptor(4, "Goldfish 3-axis Gyroscope", "Android"),
    WearSensorDescriptor(2, "Goldfish 3-axis Magnetic field sensor", "Android"),
)

@Preview(
    device = README_WEAR_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
private annotation class ReadmeWearPreview

private const val README_WEAR_DEVICE = "spec:width=454px,height=454px,dpi=320,isRound=true"
