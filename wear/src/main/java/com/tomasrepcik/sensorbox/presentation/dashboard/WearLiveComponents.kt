package com.tomasrepcik.sensorbox.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.tomasrepcik.sensorbox.R
import com.tomasrepcik.sensorbox.core.format.ValueFormats
import com.tomasrepcik.sensorbox.domain.sensors.WatchSensorDescriptor
import com.tomasrepcik.sensorbox.presentation.WearListScreen
import com.tomasrepcik.sensorbox.presentation.WearNavigationRow
import com.tomasrepcik.sensorbox.presentation.WearPageTitle

@Composable
internal fun WearLiveSensor(
    sensor: WatchSensorDescriptor,
    latestValue: Float?,
    chartModelProducer: CartesianChartModelProducer,
    accept: (WearDashboardIntent) -> Unit,
) {
    AppScaffold {
        ScreenScaffold { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WearLiveReading(sensor.name, latestValue)
                Spacer(Modifier.height(8.dp))
                WearLiveChart(chartModelProducer)
                Spacer(Modifier.height(8.dp))
                WearLiveBackButton { accept(WearDashboardIntent.Back) }
            }
        }
    }
}

@Composable
private fun WearLiveReading(sensorName: String, latestValue: Float?) {
    Text(
        text = sensorName,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        maxLines = 2,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = latestValue?.let(ValueFormats::decimal) ?: stringResource(R.string.waiting),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun WearLiveChart(chartModelProducer: CartesianChartModelProducer) {
    CartesianChartHost(
        chart = rememberCartesianChart(rememberLineCartesianLayer()),
        modelProducer = chartModelProducer,
        modifier = Modifier.fillMaxWidth().height(82.dp),
    )
}

@Composable
private fun WearLiveBackButton(onBack: () -> Unit) {
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.back))
    }
}

@Composable
internal fun WearSensorPicker(sensors: List<WatchSensorDescriptor>, accept: (WearDashboardIntent) -> Unit) {
    WearListScreen { transformation ->
        item { WearPageTitle(stringResource(R.string.live_values), transformation) }
        sensors.forEach { sensor ->
            item {
                WearNavigationRow(
                    label = sensor.name,
                    detail = sensor.vendor,
                    icon = R.drawable.ic_sensor,
                    transformation = transformation,
                ) { accept(WearDashboardIntent.ObserveSensor(sensor.type)) }
            }
        }
        item { WearBackButton(transformation, accept) }
    }
}
